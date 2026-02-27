# SRTP Java Panama (FFM API) Project

This project provides high-performance Java bindings for the [libsrtp](https://github.com/cisco/libsrtp) library using Java's Foreign Function & Memory (FFM) API (Project Panama). It allows Java applications to leverage the industry-standard Secure Real-time Transport Protocol (SRTP) implementation with minimal overhead.

## Test Demos

The `demo` module contains a suite of Java applications that are direct ports of the original C test drivers found in the `libsrtp` repository. These demos serve both as verification tests and as implementation examples for developers.

### Ported Test Drivers

| Java Demo | Original C Driver | Feature Tested |
| :--- | :--- | :--- |
| **KernelDemo** | `kernel_driver.c` | Crypto kernel initialization, cipher/auth allocation, and library shutdown. |
| **AuthDemo** | `auth_driver.c` | HMAC-SHA1 authentication against NIST test vectors. |
| **ReplayDemo** | `replay_driver.c` | Basic anti-replay database (`srtp_rdb_t`) and replay window logic. |
| **CipherDemo** | `cipher_driver.c` | AES-ICM (Integer Counter Mode) encryption and decryption cycles. |
| **RdbxDemo** | `rdbx_driver.c` | Extended replay database (`srtp_rdbx_t`) with Rollover Counter (ROC) management. |
| **ROC_DriverDemo** | `roc_driver.c` | Deep-level verification of sequence number index guessing and rollover handling. |
| **SrtpDemo** | `srtp_driver.c` | Full SRTP lifecycle: policy setup, session creation, packet protection, and unprotection. |

### Technical Details for Developers

These demos demonstrate the core patterns required to use the native library safely and efficiently:

- **Arena-based Memory Management**: All demos use `java.lang.foreign.Arena` to manage the lifecycle of native memory. This ensuring that structs like `srtp_policy_t` are automatically deallocated when no longer needed, preventing memory leaks.
- **Foreign Function Mapping**: Native C functions are accessed through generated bindings (`srtp_h`). The demos show how to handle C-style return codes (`srtp_err_status_ok`) and how to pass Java `MemorySegment` objects as pointers.
- **Session Orchestration**: `SrtpDemo` specifically shows the critical distinction between sender and receiver sessions, including how to configure `ssrc_any_inbound` for receiving streams.

## Running the Demos

### Prerequisites

- **Java SDK**: Version 22 or later is required for the Foreign Function & Memory API.
- **libsrtp**: The native library must be compiled as a shared object (`.dll` on Windows, `.so` on Linux/macOS).
- **Maven**: For building the project.

### Build Instructions

From the root project directory:

```powershell
mvn clean install
```

### Execution

To run a demo, you must enable preview features and provide the path to the native library. For example, to run the full SRTP demo:

```powershell
java --enable-preview --enable-native-access=ALL-UNNAMED `
  -Djava.library.path="path/to/libsrtp/build" `
  -classpath "demo/target/classes;java-srtp/target/classes" `
  demo.io.github.kinsleykajiva.SrtpDemo
```

*Note: Replace `path/to/libsrtp/build` with the actual directory containing `libsrtp3.dll`.*

## Common Pitfalls and Lessons Learned

Developing these Java bindings and demos revealed several important nuances of the Foreign Function & Memory API and native library integration:

### 1. `allocate` vs `allocateFrom` (Project Panama)
When passing the address of a variable to a native function that expects to read and/or write to it (e.g., `size_t *len`), ensure the memory is correctly initialized.
- **Incorrect**: `arena.allocate(C_LONG_LONG, 1024)` only allocates space; the value at that address is uninitialized (often zero). If the native library uses this value as an input (like an available buffer size), it will fail with "buffer too small".
- **Correct**: `arena.allocateFrom(C_LONG_LONG, 1024L)` allocates space **and** initializes it with the value 1024.

### 2. Octal Literals in Java
Java treats integer literals starting with `0` as octal. This can cause subtle bugs when matching SSRCs or other identifiers.
- **Example**: `01234` is not 1234 decimal; it is `1 * 8^3 + 2 * 8^2 + 3 * 8^1 + 4 * 8^0 = 668`.
- **Lesson**: Always use decimal literals (e.g., `1234`) or hexadecimal (e.g., `0x04D2`) for clarity and correctness when matching native values.

### 3. Session Context Separation
In SRTP, a single session context (`srtp_t`) can behave differently depending on whether it's used for protecting (sending) or unprotecting (receiving). 
- **Lesson**: For testing "loopback" style communication within a single JVM, it is critical to create two separate sessions: one configured as a sender (`ssrc_specific`) and one as a receiver (`ssrc_any_inbound`). Using the same session for both operations on the same packet will lead to authentication or replay-protection errors.

## Project Structure

- `java-srtp`: Contains the low-level FFM bindings generated for `libsrtp`.
- `demo`: Contains the test demos and usage examples.
- `libsrtp`: The original C library source (used for building the native binary).
