import java.io.ByteArrayOutputStream;
import java.util.Map;

class BlindCommitmentContext {
    public byte[] commitment;
    public byte[] proof;
    public byte[] blinding_factor;

    public BlindCommitmentContext(byte[] commitment, byte[] proof, byte[] blinding_factor) {
        this.commitment = commitment;
        this.proof = proof;
        this.blinding_factor = blinding_factor;
    }
}

class Bbs {
    private static native int bls_generate_g1_key(byte[] seed, byte[] public_key, byte[] secret_key);
    private static native int bls_generate_g2_key(byte[] seed, byte[] public_key, byte[] secret_key);
    private static native int bls_generate_blinded_g1_key(byte[] seed, byte[] blinding_factor, byte[] public_key, byte[] secret_key);
    private static native int bls_generate_blinded_g2_key(byte[] seed, byte[] blinding_factor, byte[] public_key, byte[] secret_key);
    private static native int bls_secret_key_to_bbs_key(byte[] secret_key, int message_count, ByteArrayOutputStream public_key);
    private static native int bls_public_key_to_bbs_key(byte[] short_public_key, int message_count, ByteArrayOutputStream public_key);

    private static native long bbs_sign_init();
    private static native int bbs_sign_set_secret_key(long handle, byte[] secret_key);
    private static native int bbs_sign_set_public_key(long handle, byte[] public_key);
    private static native int bbs_sign_add_message_bytes(long handle, byte[] message);
    private static native int bbs_sign_add_message_prehashed(long handle, byte[] hash);
    private static native int bbs_sign_finish(long handle, byte[] signature);

    private static native long bbs_verify_init();
    private static native int bbs_verify_add_message_bytes(long handle, byte[] message);
    private static native int bbs_verify_add_message_prehashed(long handle, byte[] hash);
    private static native int bbs_verify_set_public_key(long handle, byte[] public_key);
    private static native int bbs_verify_set_signature(long handle, byte[] signature);
    private static native int bbs_verify_finish(long handle);

    private static native int bbs_blind_signature_size();
    private static native long bbs_blind_commitment_init();
    private static native int bbs_blind_commitment_add_message_bytes(long handle, int index, byte[] message);
    private static native int bbs_blind_commitment_add_prehashed(long handle, int index, byte[] hash);
    private static native int bbs_blind_commitment_set_public_key(long handle, byte[] public_key);
    private static native int bbs_blind_commitment_set_nonce_bytes(long handle, byte[] nonce);
    private static native byte[] bbs_blind_commitment_finish(long handle, byte[] commitment, byte[] blinding_factor);

    private static native long bbs_blind_sign_init();
    private static native int bbs_blind_sign_set_secret_key(long handle, byte[] secret_key);
    private static native int bbs_blind_sign_set_public_key(long handle, byte[] public_key);
    private static native int bbs_blind_sign_set_commitment(long handle, byte[] commitment);
    private static native int bbs_blind_sign_add_message_bytes(long handle, int index, byte[] message);
    private static native int bbs_blind_sign_add_prehashed(long handle, int index, byte[] hash);
    private static native int bbs_blind_sign_finish(long handle, byte[] blind_signature);

    public static byte[] bbs_sign(byte[] secret_key, byte[] public_key, byte[][] messages) throws Exception {
        long handle = bbs_sign_init();
        if (0 == handle) {
            throw new Exception("Unable to create signing context");
        }
        if (0 == bbs_sign_set_secret_key(handle, secret_key)) {
            throw new Exception("Unable to set secret key");
        }
        if (0 == bbs_sign_set_public_key(handle, public_key)) {
            throw new Exception("Unable to set public key");
        }
        for (byte[] msg : messages) {
            if (0 == bbs_sign_add_message_bytes(handle, msg)) {
                throw new Exception("Unable to add message");
            }
        }
        byte[] signature = new byte[96];
        if (0 == bbs_sign_finish(handle, signature)) {
            throw new Exception("Unable to create signature");
        }
        return signature;
    }

    public static boolean bbs_verify(byte[] public_key, byte[] signature, byte[][] messages) throws Exception {
        long handle = bbs_verify_init();
        if (0 == handle) {
            throw new Exception("Unable to create verify signature context");
        }
        if (0 == bbs_verify_set_public_key(handle, public_key)) {
            throw new Exception("Unable to set public key");
        }
        if (0 == bbs_verify_set_signature(handle, signature)) {
            throw new Exception("Unable to set signature");
        }
        for (byte[] msg : messages) {
            if (0 == bbs_verify_add_message_bytes(handle, msg)) {
                throw new Exception("Unable to add message");
            }
        }
        int res = bbs_verify_finish(handle);
        switch (res) {
            case 1: return true;
            case 0: return false;
            default: throw new Exception("Unable to verify signature");
        }
    }

    public static BlindCommitmentContext bbs_blind_commitment(byte[] public_key, Map<Integer, byte[]> messages, byte[] nonce) throws Exception {
        long handle = bbs_blind_commitment_init();
        if (0 == handle) {
            throw new Exception("Unable to create blind commitment context");
        }
        if (0 == bbs_blind_commitment_set_public_key(handle, public_key)) {
            throw new Exception("Unable to set public key");
        }
        if (0 == bbs_blind_commitment_set_nonce_bytes(handle, nonce)) {
            throw new Exception("Unable to set nonce");
        }
        for (Map.Entry<Integer,byte[]> entry : messages.entrySet()) {
            if (0 == bbs_blind_commitment_add_message_bytes(handle, entry.getKey(), entry.getValue())) {
                throw new Exception("Unable to add message");
            }
        }
        byte[] blinding_factor = new byte[32];
        byte[] commitment = new byte[48];
        byte[] proof = bbs_blind_commitment_finish(handle, commitment, blinding_factor);
        if (proof == null || proof.length == 0) {
            throw new Exception("Unable to create blind commitment");
        }
        BlindCommitmentContext context = new BlindCommitmentContext(commitment, proof, blinding_factor);
        return context;
    }

    public static byte[] bbs_blind_sign(byte[] secret_key, byte[] public_key, byte[] commitment, Map<Integer, byte[]> messages) throws Exception {
        long handle = bbs_blind_sign_init();
        if (0 == handle)
            throw new Exception("Unable to create blind sign context");
        if (0 == bbs_blind_sign_set_secret_key(handle, secret_key))
            throw new Exception("Unable to set secret key");
        if (0 == bbs_blind_sign_set_public_key(handle, public_key))
            throw new Exception("Unable to set public key");
        if (0 == bbs_blind_sign_set_commitment(handle, commitment))
            throw new Exception("Unable to set commitment");
        for (Map.Entry<Integer,byte[]> entry : messages.entrySet()) {
            if (0 == bbs_blind_sign_add_message_bytes(handle, entry.getKey(), entry.getValue())) {
                throw new Exception("Unable to add message");
            }
        }
        byte[] blind_signature = new byte[bbs_blind_signature_size()];
        if (0 == bbs_blind_sign_finish(handle, blind_signature))
            throw new Exception("Unable to create blind signature");
        return blind_signature;
    }

    static {
        System.loadLibrary("bbs");
    }

    public static void main(String[] args) {
        byte[] seed = new byte[32];
        byte[] blinding_factor = new byte[32];
        byte[] public_key = new byte[96];
        byte[] secret_key = new byte[32];
        bls_generate_blinded_g1_key(seed, blinding_factor, public_key, secret_key);
        System.out.println("Bbs");
    }

    public void write(byte[] arr, int off, int len) {

    }
}