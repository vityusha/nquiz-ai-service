# Installing NQuiz AI Service on VPS using GitHub Actions

### 1. Preparing VPS

Log in to your server via SSH as root:

```bash
ssh root@YOUR_SERVER_IP
```

```bash
adduser deploy

sudo apt update
sudo apt install apt-transport-https ca-certificates curl gnupg lsb-release

sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER

su - deploy

sudo mkdir -p /opt/nquiz-ai-service
sudo chown deploy:deploy /opt/nquiz-ai-service
```

Make shure that user ID is 1000 and group ID is 1000,

Create _/opt/nquiz-ai-service/docker-compose.yml_

```yaml
services:
  app:
    image: ghcr.io/YOUR_GITHUB_USERNAME/nquiz-ai-service:latest
    ports:
      - "8080:8080"
    volumes:
      - ./data:/var/lib/nquiz-ai-service/data
      - ./config:/nquiz-ai-service/config:ro
    env_file:
      - .env
    environment:
      MICRONAUT_ENVIRONMENTS: prod
      MICRONAUT_CONFIG_FILES: /nquiz-ai-service/config/application.yml
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://127.0.0.1:8080/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    restart: unless-stopped

  # Log via ssh -L 8080:localhost:8080 deploy@SERVER_IP and http://localhost:8080
  dozzle:
    image: amir20/dozzle:latest
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    restart: unless-stopped
```

### 2. Authorization in GitHub Container Registry (GHCR)

Since your image is private (stored in your private GitHub repository), Docker on the VPS must have permission to download it.

- Go to **GitHub -> Settings -> Developer settings -> Personal access tokens -> Tokens (classic)**.
- Click **Generate new token (classic)**.
- Give it a name (e.g., "VPS Deploy").
- Select scopes (permissions): be sure to check the read:packages box.
- Generate a token and COPY IT (it won't appear again).

Return to the VPS terminal (you're still using the deploy user) and log in:

```bash
echo "YOUR_COPIED_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

You should see **"Login Succeeded."**

### 3. Configuring SSH keys for GitHub Actions

Now you need to allow GitHub to access your VPS via SSH.

On your computer (not the VPS!), generate a separate key pair for deployment:
bash

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy
```

Press Enter (do not set a password).

Copy the public key to your VPS:

```bash
ssh-copy-id -i ~/.ssh/github_deploy.pub deploy@YOUR_SERVER_IP
```

(It will ask for the password for the deploy user you created in Step 2)

Copy the private key to GitHub Secrets:
Display it:

```bash
cat ~/.ssh/github_deploy
```

Copy the entire contents (from -----BEGIN... to -----END...).

- Go to your **GitHub repository -> Settings -> Secrets and variables -> Actions**.
- Create four secrets:
- **VPS_HOST** = Your server's IP address
- **VPS_PORT** = Your server's SSH port
- **VPS_USER** = deploy
- **VPS_SSH_KEY** = Paste the copied private key here

### 4. Service configs

Create the _/opt/nquiz-ai-service/.env_ on the server with appropriate values.

```ini
# Copy to .env and fill in real values.
# Never commit .env to git.

# SQLite database path inside the container
DB_URL=jdbc:sqlite:/var/lib/nquiz-ai-service/data/nquiz-ai-users.db

# LLM provider credentials (set only the providers you use)
GROQ_API_KEY=
OPENAI_API_KEY=
GEMINI_API_KEY=
DEEPSEEK_API_KEY=

# Production image override (optional, used by docker-compose.prod.yml)
# IMAGE=ghcr.io/your-org/nquiz-ai-service:latest
```

Create a folder with Micronaut configs:

```bash
cd /opt/nquiz-ai-service
mkdir config
nano config/application-prod.yml
```

Paste the **minimal** production config here. The most important thing is to correctly specify the path to the SQLite database (use the absolute path inside the container - /var/lib/nquiz-ai-service/data/):

```yaml
micronaut:
  application:
    name: nquiz-ai-service

datasources:
  default:
    url: jdbc:sqlite:/var/lib/nquiz-ai-service/data/nquiz-ai-users.db
    driver-class-name: org.sqlite.JDBC

flyway:
  enabled: true
  locations:
    - classpath:db/migration

logger:
  levels:
    com.lainlab: INFO
    root: WARN
```

If you have specific settings for the prod environment, name the file application-prod.yml and don't forget to add prod to the MICRONAUT_ENVIRONMENTS variable in docker-compose.yml on the server.

### 5. Configure reverse proxy and HTTPS

#### Install Nginx web server:

```bash
sudo apt update
sudo apt install nginx -y
```

#### Firewall (UFW) Setup:

We'll only open standard web ports (80 and 443) and close 8080 externally to prevent anyone from accessing your backend without using Nginx.

```bash
sudo apt install ufw -y
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
# IMPORTANT: DO NOT use ufw allow 8080.
sudo ufw enable
```

#### Configuring Reverse Proxy in Nginx:

Remove default site:

```bash
sudo rm /etc/nginx/sites-enabled/default
```

Create a configuration file for your service:

Create file _/etc/nginx/sites-available/nquiz-ai-service_

Paste the following code (replace _api.yourdomain.com_ with your real domain or IP address):

```nginx
server {
        listen 80;
        server_name api.yourdomain.com; # YOUR DOMAIN OR IP

        location / {
                # Forward traffic to port 8080, which we forwarded in docker-compose
                proxy_pass http://127.0.0.1:8080;

                # Pass headers about the real client to Micronaut
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
        }
}
```

#### Activate the configuration and restart Nginx:

```bash
sudo ln -s /etc/nginx/sites-available/nquiz-ai-service /etc/nginx/sites-enabled/
sudo nginx -t # Check for syntax errors
sudo systemctl reload nginx
```

At this point, if you access _http://api.yourdomain.com/health_, you should see a response from Micronaut, but without the lock (HTTP). 

#### Adding HTTPS (Let's Encrypt):

For free and automatic HTTPS, use the certbot utility. It will automatically modify the Nginx configuration and renew certificates.

```bash
sudo apt install certbot python3-certbot-nginx -y
```

Start obtaining the certificate (substitute your domain again):

```bash
sudo certbot --nginx -d api.yourdomain.com
```

Certbot will ask you a couple of questions:

Enter your email for notifications (required).
Agree to the terms (Y).
It will ask: Redirect HTTP to HTTPS? - Be sure to press 2 (Redirect).

Done! Certbot will register the keys in Nginx, enable redirection from http:// to https://, and configure automatic certificate renewal (a cron task is created automatically).

*NOTE:* On modern Debian systems, you do not need to manually configure a crontab entry for Certbot because automatic renewals are natively handled by a systemd timer. Debian utilizes systemd timers to handle background certificate renewals. You can verify that your automated renewal is already scheduled and active by running the following command:

```bash
sudo systemctl list-timers | grep certbot
```

#### Important Change in Micronaut (Or Not?)

Since your service is now behind Nginx (which terminates HTTPS and forwards requests internally over HTTP), Micronaut may not be aware that a secure connection is being used externally. This is important if you're generating links or using OAuth.

In your application.yml on your VPS (in the _/opt/nquiz-ai-service/config/_ folder), you can add:

```yaml
micronaut:
  server:
    # Tell Micronaut to trust headers from Nginx
    forwarded-header-strategy: native
```

(In Micronaut 4, the *X-Forwarded-\** headers we specified in Nginx are already processed correctly by default, but this line will make the behavior 100% explicit).

### 6. Autostart docker

```bash
sudo systemctl enable docker
sudo systemctl start docker
```

### 7. Check if everything works

In the terminal, enter:

```bash
curl -fsSL https://api.yourdomain.com/health
```

or in the browser address bar:

```https://api.yourdomain.com/health```

You should get something like:

```js
{"status":"UP"}
```

That's it!