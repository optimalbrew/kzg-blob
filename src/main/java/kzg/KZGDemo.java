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
        Path blobPath = Paths.get("src/main/resources/blob.bin");
        byte[] blob = Files.readAllBytes(blobPath);
        if (blob.length != CKZG4844JNI.BYTES_PER_BLOB) {
            throw new RuntimeException("Unexpected blob size: " + blob.length);
        }

        // The expected commitment and proof from Blobscan for the requested blob
        String expectedCommitmentHex = "afcd8bc80163cab0a92df605c4c071a938df9d0163ae0eb731974d47c4adf3f69d3da2827ede9cb697b302044edb7b14";
        String expectedProofHex = "890fe185e115ec215929fde001b60c3dfeff38b85130a54ea468520b1ab23fd3af00acb5c62a245ad8a8dd0f7c91e6c1";

        byte[] expectedCommitment = hexStringToByteArray(expectedCommitmentHex);
        byte[] expectedProof = hexStringToByteArray(expectedProofHex);

        System.out.println("Verifying BlobKZGProof...");
        boolean isValid = CKZG4844JNI.verifyBlobKzgProof(blob, expectedCommitment, expectedProof);
        System.out.println("verifyBlobKzgProof valid? " + isValid);

        System.out.println("Verifying BlobKZGProofBatch...");
        boolean isBatchValid = CKZG4844JNI.verifyBlobKzgProofBatch(blob, expectedCommitment, expectedProof, 1);
        System.out.println("verifyBlobKzgProofBatch valid? " + isBatchValid);

        System.out.println("Computing KZG Commitment from blob...");
        byte[] computedCommitment = CKZG4844JNI.blobToKzgCommitment(blob);
        boolean commitMatch = Arrays.equals(computedCommitment, expectedCommitment);
        System.out.println("blobToKzgCommitment matches expected? " + commitMatch);

        System.out.println("Computing Blob KZG Proof from blob...");
        byte[] computedProof = CKZG4844JNI.computeBlobKzgProof(blob, computedCommitment);
        boolean proofMatch = Arrays.equals(computedProof, expectedProof);
        System.out.println("computeBlobKzgProof matches expected? " + proofMatch);
        if (!proofMatch) {
            System.out.println("Expected proof: " + expectedProofHex);
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
