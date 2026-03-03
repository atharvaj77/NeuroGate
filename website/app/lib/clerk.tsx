'use client'

// Re-export real Clerk components — all existing imports continue to work.
export {
  ClerkProvider,
  SignedIn,
  SignedOut,
  SignInButton,
  UserButton,
  SignIn,
  useAuth,
} from '@clerk/nextjs'
