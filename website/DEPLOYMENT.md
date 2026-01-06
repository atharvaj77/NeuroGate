# Deployment Guide

This guide covers multiple deployment options for the NeuroGate landing page.

## Quick Deploy Options

### Option 1: Vercel (Recommended - Easiest)

Vercel is the company behind Next.js and provides the best deployment experience.

1. **Push to GitHub**:
   ```bash
   cd website
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/neurogate-website
   git push -u origin main
   ```

2. **Deploy on Vercel**:
   - Visit [vercel.com](https://vercel.com)
   - Click "Import Project"
   - Connect your GitHub repository
   - Vercel auto-detects Next.js and deploys
   - Get instant HTTPS URL: `https://neurogate.vercel.app`

3. **Custom Domain** (Optional):
   - Add your custom domain in Vercel dashboard
   - Update DNS records as instructed
   - Automatic SSL certificate

**Advantages**:
- Zero configuration
- Automatic deployments on git push
- Free SSL certificate
- Global CDN
- Built-in analytics

---

### Option 2: GitHub Pages (Free Static Hosting)

Perfect for portfolio projects and free hosting.

1. **Build Static Site**:
   ```bash
   cd website
   npm install
   npm run build
   ```

2. **Deploy to GitHub Pages**:

   **Option A: Using gh-pages package**:
   ```bash
   npm install -g gh-pages

   # Deploy the 'out' directory
   gh-pages -d out
   ```

   **Option B: Manual deployment**:
   ```bash
   # In your main NeuroGate repo
   git checkout -b gh-pages
   git rm -rf .
   cp -r website/out/* .
   git add .
   git commit -m "Deploy website"
   git push origin gh-pages
   ```

3. **Enable GitHub Pages**:
   - Go to repository Settings → Pages
   - Source: Deploy from branch `gh-pages`
   - Your site will be at: `https://yourusername.github.io/neurogate`

**Advantages**:
- Completely free
- Good for portfolio/resume links
- Easy to update

---

### Option 3: Netlify (Easy with Great Features)

Netlify offers excellent CI/CD and free hosting.

1. **Build the Site**:
   ```bash
   cd website
   npm run build
   ```

2. **Deploy Options**:

   **Option A: Drag & Drop** (Easiest):
   - Visit [netlify.com](https://netlify.com)
   - Drag the `out/` folder to deploy
   - Instant deployment with HTTPS

   **Option B: Netlify CLI**:
   ```bash
   npm install -g netlify-cli
   netlify login
   netlify init
   netlify deploy --prod --dir=out
   ```

   **Option C: Git Integration**:
   - Connect your GitHub repo
   - Build command: `npm run build`
   - Publish directory: `out`
   - Auto-deploy on push

**Advantages**:
- Free SSL
- Form handling
- Serverless functions
- Split testing
- Custom domains

---

### Option 4: AWS S3 + CloudFront (Professional)

For enterprise-grade hosting with AWS.

1. **Build the Site**:
   ```bash
   npm run build
   ```

2. **Create S3 Bucket**:
   ```bash
   aws s3 mb s3://neurogate-website
   aws s3 website s3://neurogate-website --index-document index.html
   ```

3. **Upload Files**:
   ```bash
   aws s3 sync out/ s3://neurogate-website --acl public-read
   ```

4. **Configure CloudFront** (Optional but recommended):
   - Create CloudFront distribution
   - Origin: Your S3 bucket
   - Enable SSL with ACM certificate
   - Set up custom domain

**Advantages**:
- Enterprise-grade reliability
- Global CDN
- Fine-grained control
- Integrates with AWS services

---

## Custom Domain Setup

### 1. Purchase Domain (if needed)
- Namecheap
- Google Domains
- GoDaddy

### 2. Configure DNS

For **Vercel**:
```
A     @      76.76.21.21
CNAME www    cname.vercel-dns.com
```

For **Netlify**:
```
A     @      75.2.60.5
CNAME www    your-site.netlify.app
```

For **GitHub Pages**:
```
A     @      185.199.108.153
A     @      185.199.109.153
A     @      185.199.110.153
A     @      185.199.111.153
CNAME www    yourusername.github.io
```

### 3. SSL Certificate
All platforms provide free SSL via Let's Encrypt automatically.

---

## Automated Deployment with GitHub Actions

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy Website

on:
  push:
    branches: [ main ]
    paths:
      - 'website/**'

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'

    - name: Install dependencies
      run: |
        cd website
        npm install

    - name: Build
      run: |
        cd website
        npm run build

    - name: Deploy to GitHub Pages
      uses: peaceiris/actions-gh-pages@v3
      with:
        github_token: ${{ secrets.GITHUB_TOKEN }}
        publish_dir: ./website/out
```

---

## Performance Optimization

### Before Deployment

1. **Optimize Images**:
   - Use WebP format
   - Compress with TinyPNG
   - Add to `public/` folder

2. **Lighthouse Check**:
   ```bash
   npm install -g lighthouse
   lighthouse http://localhost:3000 --view
   ```

3. **Bundle Analysis**:
   ```bash
   npm install @next/bundle-analyzer
   # Add to next.config.js
   ANALYZE=true npm run build
   ```

### After Deployment

1. **Test Loading Speed**:
   - [PageSpeed Insights](https://pagespeed.web.dev/)
   - [GTmetrix](https://gtmetrix.com/)
   - [WebPageTest](https://www.webpagetest.org/)

2. **SEO Check**:
   - Google Search Console
   - Submit sitemap
   - Verify meta tags

---

## Monitoring

### Vercel Analytics
- Built-in Web Vitals tracking
- Real user metrics
- Free tier included

### Google Analytics
Add to `app/layout.tsx`:

```tsx
import Script from 'next/script'

// In the component:
<Script
  src="https://www.googletagmanager.com/gtag/js?id=GA_MEASUREMENT_ID"
  strategy="afterInteractive"
/>
```

---

## Troubleshooting

### Build Errors

```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npm run build
```

### Deployment Issues

1. **Check build logs** in your deployment platform
2. **Verify all dependencies** are in `package.json`
3. **Test locally** with `npm run build && npm start`

### 404 Errors

- Ensure `next.config.js` has `output: 'export'`
- Check `.gitignore` doesn't exclude build files
- Verify `out/` directory is created

---

## Resume/LinkedIn Setup

1. **Add Live URL**:
   - LinkedIn Projects section
   - Resume under "NeuroGate Enterprise"
   - Format: `Live: neurogate.vercel.app | GitHub: github.com/you/neurogate`

2. **Take Screenshots**:
   - Hero section
   - Features section
   - Architecture diagram
   - Use for portfolio/presentations

3. **Update Links**:
   - In `app/page.tsx`: Update GitHub and LinkedIn URLs
   - In `app/docs/page.tsx`: Update GitHub URL
   - Commit and redeploy

---

## Recommended Deployment Flow

For professional presentation on resume/LinkedIn:

1. **Deploy on Vercel** (best performance + free)
2. **Set up custom domain** (optional but professional)
3. **Enable analytics** (track visitors)
4. **Add to LinkedIn Projects**:
   - Title: "NeuroGate Enterprise - AI Gateway"
   - URL: Your deployed site
   - Description: Use resume bullets from docs page
5. **GitHub README**: Link to live demo at top

---

## Cost Breakdown

| Platform | Free Tier | Custom Domain | SSL | CDN |
|----------|-----------|---------------|-----|-----|
| Vercel | ✅ Unlimited | ✅ Free | ✅ Auto | ✅ Global |
| Netlify | ✅ 100GB/mo | ✅ Free | ✅ Auto | ✅ Global |
| GitHub Pages | ✅ 1GB storage | ✅ Free | ✅ Auto | ❌ Limited |
| AWS S3+CF | ❌ ~$1-5/mo | ~$12/yr | ✅ Free (ACM) | ✅ Global |

**Recommendation**: Start with Vercel (free + best performance)

---

## Next Steps

1. Choose deployment platform
2. Deploy the site
3. Get your live URL
4. Update GitHub/LinkedIn links in the code
5. Redeploy with correct links
6. Add live URL to your resume/LinkedIn
7. Share with potential employers!

---

**Questions?** Check the platform documentation:
- [Vercel Docs](https://vercel.com/docs)
- [Netlify Docs](https://docs.netlify.com)
- [GitHub Pages Docs](https://docs.github.com/en/pages)
