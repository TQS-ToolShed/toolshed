FROM node:22-alpine

WORKDIR /web_app

COPY package.json package-lock.json ./

RUN npm install --silent

COPY . .

EXPOSE 5173
CMD npm run dev -- --port 5173 --strictPort --host 0.0.0.0