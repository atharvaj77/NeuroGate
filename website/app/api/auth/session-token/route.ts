import { cookies } from 'next/headers'
import { NextResponse } from 'next/server'

const COOKIE_NAME = 'ng_jwt'

export async function POST(request: Request) {
  try {
    const body = await request.json()
    const token = typeof body?.token === 'string' ? body.token : ''

    if (!token) {
      return NextResponse.json({ error: 'missing_token' }, { status: 400 })
    }

    cookies().set(COOKIE_NAME, token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: 60 * 60, // 1 hour
    })

    return NextResponse.json({ ok: true })
  } catch {
    return NextResponse.json({ error: 'invalid_payload' }, { status: 400 })
  }
}

export async function DELETE() {
  cookies().delete(COOKIE_NAME)
  return NextResponse.json({ ok: true })
}
