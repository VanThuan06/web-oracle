# ==============================================================================
# 1. BUILD STAGE (Node 20 Alpine)
# ==============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies (cached if package files don't change)
COPY package*.json ./
RUN npm ci

# Copy project source and build Angular app
COPY . .
RUN npm run build

# ==============================================================================
# 2. RUNTIME STAGE (Nginx Alpine Web Server)
# ==============================================================================
FROM nginx:alpine

# Remove default nginx html
RUN rm -rf /usr/share/nginx/html/*

# Copy built Angular artifacts (Angular 17+ outputs to browser folder)
COPY --from=builder /app/dist/newmen-storefront/browser /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 4220

HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O - http://localhost:4220/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
