package kzg;

import ethereum.ckzg4844.CKZG4844JNI;
import ethereum.ckzg4844.ProofAndY;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class KZGDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Loading native library...");
        CKZG4844JNI.loadNativeLibrary();

        System.out.println("Loading trusted setup...");
        CKZG4844JNI.loadTrustedSetup("src/main/resources/trusted_setup.txt", 0);

        System.out.println("Reading blob data...");
        Path blobPath1 = Paths.get("src/main/resources/blob.bin");
        byte[] blob1 = Files.readAllBytes(blobPath1);
        Path blobPath2 = Paths.get("src/main/resources/blob2.bin");
        byte[] blob2 = Files.readAllBytes(blobPath2);
        
        if (blob1.length != CKZG4844JNI.BYTES_PER_BLOB || blob2.length != CKZG4844JNI.BYTES_PER_BLOB) {
            throw new RuntimeException("Unexpected blob size");
        }

        // The expected commitment and proof from Blobscan for the requested blobs
        String expectedCommitmentHex1 = "afcd8bc80163cab0a92df605c4c071a938df9d0163ae0eb731974d47c4adf3f69d3da2827ede9cb697b302044edb7b14";
        String expectedProofHex1 = "890fe185e115ec215929fde001b60c3dfeff38b85130a54ea468520b1ab23fd3af00acb5c62a245ad8a8dd0f7c91e6c1";
        
        String expectedCommitmentHex2 = "abea2993faf9f7b26a840e426026137c3b410c14c158a9f9d92d3ce81c548dd35f8a65aeafc6727e598fcdc99dda6d7f";
        String expectedProofHex2 = "86e9e03b1f37ed07f444558b0c157e00983cd19a67bb914287ba1f23afe774c4d10581dd2b2371fe88cb5e6a2880ed24";

        byte[] expectedCommitment1 = hexStringToByteArray(expectedCommitmentHex1);
        byte[] expectedProof1 = hexStringToByteArray(expectedProofHex1);

        byte[] expectedCommitment2 = hexStringToByteArray(expectedCommitmentHex2);
        byte[] expectedProof2 = hexStringToByteArray(expectedProofHex2);

        System.out.println("Verifying BlobKZGProof for blob 1...");
        boolean isValid1 = CKZG4844JNI.verifyBlobKzgProof(blob1, expectedCommitment1, expectedProof1);
        System.out.println("verifyBlobKzgProof for blob 1 valid? " + isValid1);

        System.out.println("Verifying BlobKZGProof for blob 2...");
        boolean isValid2 = CKZG4844JNI.verifyBlobKzgProof(blob2, expectedCommitment2, expectedProof2);
        System.out.println("verifyBlobKzgProof for blob 2 valid? " + isValid2);

        System.out.println("Verifying BlobKZGProofBatch for both blobs...");
        byte[] batchBlobs = new byte[CKZG4844JNI.BYTES_PER_BLOB * 2];
        System.arraycopy(blob1, 0, batchBlobs, 0, CKZG4844JNI.BYTES_PER_BLOB);
        System.arraycopy(blob2, 0, batchBlobs, CKZG4844JNI.BYTES_PER_BLOB, CKZG4844JNI.BYTES_PER_BLOB);

        int commitmentLen = expectedCommitment1.length;
        byte[] batchCommitments = new byte[commitmentLen * 2];
        System.arraycopy(expectedCommitment1, 0, batchCommitments, 0, commitmentLen);
        System.arraycopy(expectedCommitment2, 0, batchCommitments, commitmentLen, commitmentLen);

        int proofLen = expectedProof1.length;
        byte[] batchProofs = new byte[proofLen * 2];
        System.arraycopy(expectedProof1, 0, batchProofs, 0, proofLen);
        System.arraycopy(expectedProof2, 0, batchProofs, proofLen, proofLen);

        boolean isBatchValid = CKZG4844JNI.verifyBlobKzgProofBatch(batchBlobs, batchCommitments, batchProofs, 2);
        System.out.println("verifyBlobKzgProofBatch for 2 blobs valid? " + isBatchValid);

        // Keep variables for single-blob later assertions to not break the rest of the file
        boolean isValid = isValid1 && isValid2;
        byte[] blob = blob1;
        byte[] expectedCommitment = expectedCommitment1;
        byte[] expectedProof = expectedProof1;

        System.out.println("Computing KZG Commitment from blob...");
        byte[] computedCommitment = CKZG4844JNI.blobToKzgCommitment(blob);
        boolean commitMatch = Arrays.equals(computedCommitment, expectedCommitment);
        System.out.println("blobToKzgCommitment matches expected? " + commitMatch);

        System.out.println("Computing Blob KZG Proof from blob...");
        byte[] computedProof = CKZG4844JNI.computeBlobKzgProof(blob, computedCommitment);
        boolean proofMatch = Arrays.equals(computedProof, expectedProof);
        System.out.println("computeBlobKzgProof matches expected? " + proofMatch);
        if (!proofMatch) {
            System.out.println("Expected proof: " + expectedProofHex1);
            System.out.println("Computed proof: " + bytesToHex(computedProof));
        }

        System.out.println("Computing KZG Proof at a specific point z...");
        // Compute proof at point z for the polynomial represented by the blob
        // We'll just define an arbitrary point `z` (32 bytes)
        byte[] zBytes = new byte[CKZG4844JNI.BYTES_PER_FIELD_ELEMENT];
        zBytes[31] = 1; // z = 1 (in big-endian layout)
        ProofAndY proofAndY = CKZG4844JNI.computeKzgProof(blob, zBytes);

        System.out.println("Verifying KZG Proof at point z...");
        boolean isPointValid = CKZG4844JNI.verifyKzgProof(computedCommitment, zBytes, proofAndY.getY(),
                proofAndY.getProof());
        System.out.println("verifyKzgProof valid? " + isPointValid);

        System.out.println("Freeing trusted setup...");
        CKZG4844JNI.freeTrustedSetup();

        if (!isValid || !isBatchValid || !commitMatch || !proofMatch || !isPointValid) {
            System.err.println("Verification failed for one or more checks!");
            System.exit(1);
        }

        System.out.println("All KZG functions executed successfully.");
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
