# NeuroGuard: Active Defense

The `core.vault` package (NeuroGuard) implements zero-trust security for AI.

## PII Vision
We detect sensitive data before it leaves the kernel.
- **Detectors**: `ContextAwarePiiDetector.java` (Regex + Context keywords).
- **Types**: SSN, CREDIT_CARD, EMAIL, PHONE, API_KEY.

## Reversible Tokenization
We don't just mask data; we tokenize it so the LLM can still "reference" it.
1.  **Sanitize**: "Call 555-1234" -> "Call <PHONE_1>"
2.  **Vault**: Store `<PHONE_1>: 555-1234` in request-scoped memory.
3.  **Process**: LLM sees tokens, outputs "Dialing <PHONE_1>...".
4.  **Restore**: Response becomes "Dialing 555-1234...".

## Jailbreak Protection
Regex-based signatures detect prompt injection attempts (e.g., "Ignore previous instructions").
