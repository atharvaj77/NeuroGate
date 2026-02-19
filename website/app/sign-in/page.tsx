import { SignIn } from '../lib/clerk'
import Link from 'next/link'

export default function SignInPage() {
  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-16 bg-black">
      <div className="w-full max-w-md">
        <Link href="/" className="text-primary-400 hover:text-primary-300 mb-6 inline-block">
          ← Back to Home
        </Link>
        <SignIn routing="path" path="/settings" />
      </div>
    </div>
  )
}
