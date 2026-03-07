# C-KZG-4844 Java Integration Demo

This project demonstrates how to use the C-KZG-4844 library via its Java bindings (EIP-4844) using real data from Ethereum mainnet. This is for the orginal version of EIP-4844, where proofs are over blobs, not cells. Therefore, when picking a blob to verify (e.g. from Etherscan), we should pick something that is consistent with the original Dencun spec and not the latest Fusaka version.

## Prerequisites

- **Java JDK 17**
- **Unix-like OS** (macOS, Linux) for standard compilation.
- **Maven** (`mvn`) or **cURL** to fetch the Java Native Interface dependencies.

## Setup

The backend integration points are built via Gradle Wrapper (`gradlew`).

1. Download the repository (or run directly in this template directory).
2. The `trusted_setup.txt` should be available in `src/main/resources/trusted_setup.txt`. If missing, download it via:

```bash
curl -sL https://raw.githubusercontent.com/ethereum/c-kzg-4844/main/src/trusted_setup.txt -o src/main/resources/trusted_setup.txt
```

## Running the Application

To download dependencies, compile and run the Java test suite against the local data:

```bash
# Run via Gradle application plugin
./gradlew run
```

## Supported EIP-4844 KZG Functions

This application exercises the following EIP-4844 cryptographic properties from the `ethereum.ckzg4844` class:

- `blobToKzgCommitment(byte[] blob)`
- `computeBlobKzgProof(byte[] blob, byte[] commitmentBytes)`
- `verifyBlobKzgProof(byte[] blob, byte[] commitmentBytes, byte[] proofBytes)`
- `verifyBlobKzgProofBatch(byte[] blobs, byte[] commitmentsBytes, byte[] proofsBytes, long count)`
- `computeKzgProof(byte[] blob, byte[] zBytes)`
- `verifyKzgProof(byte[] commitmentBytes, byte[] zBytes, byte[] yBytes, byte[] proofBytes)`

It also demonstrates the basic initializations: `loadNativeLibrary()`, `loadTrustedSetup()`, and `freeTrustedSetup()`.

## Testing Your Own Blob Data

If you want to try verifying data of your own choosing, you can pull blobs from Ethereum blob explorers like [Blobscan.com](https://blobscan.com).

1. Find a transaction or block on Blobscan and select a specific **Blob**.
2. **Download the RAW blob binary:** In the Blobscan interface, look for the "Data Storage References" and download the `.bin` data to `src/main/resources/blob.bin`.
3. **Copy the expected Commitment and Proof Hex Strings:** Note the "Commitment" and "Proof" fields available on the Blobscan UI. 
4. **Update `KZGDemo.java`:** Edit the variables inside `src/main/java/kzg/KZGDemo.java` with your newly found data values:

```java
// Open src/main/java/kzg/KZGDemo.java and update these fields:
String expectedCommitmentHex = "<YOUR_COMMITMENT_HEX_HERE>";
String expectedProofHex = "<YOUR_PROOF_HEX_HERE>";
```

5. Recompile and run again! As long as the dataset binary matches the expected proof hashes, it will successfully cryptographically verify through the KZG EIP-4844 interface.
