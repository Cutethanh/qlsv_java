package qlsv.data;

import qlsv.security.CryptoService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DataManager implements DataHandler {
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxrefs=200000;maxbytes=50000000;"
                    + "qlsv.model.*;java.util.ArrayList;java.time.*;java.lang.*;!*");

    private final File dir;
    private final CryptoService crypto;

    public DataManager(File dir, CryptoService crypto) {
        this.dir = dir;
        this.crypto = crypto;
        dir.mkdirs();
    }

    @Override
    public <T extends Serializable> void save(List<T> items, String fileName) throws IOException {
        byte[] bytes = serialize(new ArrayList<>(items));
        byte[] signature = crypto.hmac(bytes);
        File data = new File(dir, fileName);
        File sig = signatureOf(data);
        byte[] sigText = Base64.getEncoder().encode(signature);
        writeAtomically(data, bytes);
        writeAtomically(sig, sigText);
        writeAtomically(backupOf(data), bytes);
        writeAtomically(backupOf(sig), sigText);
    }

    @Override
    public <T extends Serializable> List<T> load(String fileName, Class<T> type)
            throws IOException, IntegrityException {
        File data = new File(dir, fileName);
        if (!data.exists()) {
            return new ArrayList<>();
        }
        byte[] bytes = Files.readAllBytes(data.toPath());
        if (!crypto.verifyHmac(bytes, readSignature(signatureOf(data)))) {
            throw new IntegrityException(fileName);
        }
        Object obj = deserialize(bytes);
        if (!(obj instanceof List)) {
            throw new IntegrityException(fileName);
        }
        List<T> result = new ArrayList<>();
        for (Object o : (List<?>) obj) {
            if (!type.isInstance(o)) {
                throw new IntegrityException(fileName);
            }
            result.add(type.cast(o));
        }
        return result;
    }

    @Override
    public boolean exists(String fileName) {
        return new File(dir, fileName).exists();
    }

    @Override
    public boolean verify(String fileName) {
        File data = new File(dir, fileName);
        if (!data.exists()) {
            return false;
        }
        try {
            return crypto.verifyHmac(Files.readAllBytes(data.toPath()), readSignature(signatureOf(data)));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean restoreBackup(String fileName) {
        File data = new File(dir, fileName);
        File bak = backupOf(data);
        File bakSig = backupOf(signatureOf(data));
        if (!bak.exists() || !bakSig.exists()) {
            return false;
        }
        try {
            if (!crypto.verifyHmac(Files.readAllBytes(bak.toPath()), readSignature(bakSig))) {
                return false;
            }
            Files.copy(bak.toPath(), data.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(bakSig.toPath(), signatureOf(data).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        return bos.toByteArray();
    }

    private static Object deserialize(byte[] bytes) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(FILTER);
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Lớp không xác định trong file dữ liệu", e);
        }
    }

    private static byte[] readSignature(File sig) throws IOException {
        if (!sig.exists()) {
            return null;
        }
        try {
            String text = new String(Files.readAllBytes(sig.toPath()), StandardCharsets.US_ASCII).trim();
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void writeAtomically(File target, byte[] bytes) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        Files.write(tmp.toPath(), bytes);
        try {
            Files.move(tmp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File signatureOf(File data) {
        return new File(data.getParentFile(), data.getName() + ".sig");
    }

    private static File backupOf(File f) {
        return new File(f.getParentFile(), f.getName() + ".bak");
    }
}
