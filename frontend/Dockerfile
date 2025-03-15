FROM node:20-slim

WORKDIR /app
COPY package.json package-lock.json* ./

# Additional settings will need to be applied for frontend.
RUN npm install
COPY . .

CMD ["npm", "start"]