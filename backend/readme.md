```
sudo apt-get install openjdk-17-jdk
chmod +x gradlew
gradlew bootJar

curl -sSL https://get.docker.com | sh
sudo apt-get install -y uidmap

// might cause perf issues
// dockerd-rootless-setuptool.sh install
// sudo loginctl enable-linger $(whoami)

docker compose up --build -d
docker compose logs --follow
```