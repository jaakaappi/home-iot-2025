sudo apt-get install openjdk-17-jdk
chmod +x gradlew
gradlew bootJar

curl -sSL https://get.docker.com | sh
sudo apt-get install -y uidmap
dockerd-rootless-setuptool.sh install

docker compose up -d
docker compose logs --follow