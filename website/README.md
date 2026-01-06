# NeuroGate Landing Page

A beautiful, world-class landing page for NeuroGate Enterprise - an AI Gateway that reduces LLM costs by 40-60% while ensuring 100% PII compliance.

## Features

- **Modern Design**: Built with Next.js 14, TypeScript, and Tailwind CSS
- **Smooth Animations**: Framer Motion for professional animations and transitions
- **Fully Responsive**: Optimized for all devices (mobile, tablet, desktop)
- **SEO Optimized**: Proper meta tags and semantic HTML
- **Performance**: Static export for blazing-fast loading
- **Technical Documentation**: Comprehensive docs page with API reference

## Tech Stack

- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **Framer Motion** - Animation library
- **React Icons** - Icon library

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn/pnpm

### Installation

1. Navigate to the website directory:
```bash
cd website
```

2. Install dependencies:
```bash
npm install
# or
yarn install
# or
pnpm install
```

3. Run the development server:
```bash
npm run dev
# or
yarn dev
# or
pnpm dev
```

4. Open [http://localhost:3000](http://localhost:3000) in your browser

The page will auto-reload as you make changes.

## Building for Production

### Static Export (Recommended for GitHub Pages)

```bash
npm run build
```

This creates an `out/` directory with static HTML files that can be deployed to:
- GitHub Pages
- Netlify
- Vercel
- Any static hosting provider

### Deployment Options

#### GitHub Pages

1. Build the site:
```bash
npm run build
```

2. The `out/` directory contains your static site

3. Deploy using GitHub Actions or manually push to `gh-pages` branch

#### Vercel (Easiest)

1. Push your code to GitHub
2. Import project in Vercel
3. Deploy automatically

#### Netlify

1. Build the site: `npm run build`
2. Deploy the `out/` directory to Netlify

## Customization

### Update Your Links

Edit the following files to add your own links:

1. **GitHub Link**: Update in `app/page.tsx` and `app/docs/page.tsx`
   - Replace `https://github.com/yourusername/neurogate`

2. **LinkedIn Link**: Update in `app/page.tsx`
   - Replace `https://www.linkedin.com/in/yourprofile`

### Modify Colors

Colors are defined in `tailwind.config.js`:
- `primary`: Blue gradient colors
- `accent`: Purple gradient colors

### Add Sections

All sections are in `app/page.tsx` and `app/docs/page.tsx`. You can:
- Add new sections
- Reorder existing sections
- Modify content and styling

## Project Structure

```
website/
├── app/
│   ├── layout.tsx          # Root layout with metadata
│   ├── page.tsx            # Main landing page
│   ├── globals.css         # Global styles
│   └── docs/
│       └── page.tsx        # Documentation page
├── public/                 # Static files (images, etc.)
├── package.json            # Dependencies
├── tsconfig.json           # TypeScript config
├── tailwind.config.js      # Tailwind CSS config
└── next.config.js          # Next.js config

```

## Key Sections

### Landing Page (`/`)
- Hero with animated gradient background
- Key statistics (40-60% cost reduction, 10K+ connections, etc.)
- Problem statement (cost, privacy, vendor lock-in)
- Solution features (semantic caching, PII protection, smart routing)
- Architecture diagram
- Tech stack showcase
- Quick start guide
- CTA with GitHub/LinkedIn links

### Documentation Page (`/docs`)
- Architecture deep dive
- API reference with examples
- Configuration guides (Docker, Kubernetes)
- Metrics and monitoring
- Security features
- Production deployment checklist
- Resume bullet points

## Tips for Resume/LinkedIn

1. **Take Screenshots**: Capture key sections for your portfolio
2. **Add GitHub Link**: Make sure your GitHub repo is public
3. **Deploy Live**: Host on Vercel/Netlify for a live demo URL
4. **Update LinkedIn**: Add the live URL to your projects section
5. **Resume Bullets**: Use the provided resume bullets in the docs page

## Performance

- **Lighthouse Score**: Aim for 90+ across all metrics
- **Static Export**: No server required, instant loading
- **Code Splitting**: Automatic by Next.js
- **Image Optimization**: Use Next.js Image component for images

## License

MIT License - Feel free to use this for your portfolio!

## Support

For issues or questions:
- Check Next.js docs: https://nextjs.org/docs
- Tailwind CSS: https://tailwindcss.com/docs
- Framer Motion: https://www.framer.com/motion/

---

**Built with ❤️ for showcasing technical projects**
