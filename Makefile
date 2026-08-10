infra-auth:
	podman-compose -f auth-service/docker-compose.yml up -d

infra-chat:
	podman-compose -f chat-service/docker-compose.yml up -d

infra-trade:
	podman-compose -f trade-service/docker-compose.yml up -d

dev-auth:
	mvn clean spring-boot:run -pl auth-service 

dev-chat:
	mvn clean spring-boot:run -pl chat-service 

dev-trade:
	mvn clean spring-boot:run -pl trade-service

dev-gateway:
	mvn clean spring-boot:run -pl gateway-service 

infra-all:
	make infra-auth;
	make infra-chat;
	make infra-trade;

dev-all:
	make infra-all
	make dev-auth
	make dev-chat
	make dev-trade
	make dev-gateway

down:
	podman-compose -f auth-service/docker-compose.yml down -v
	podman-compose -f chat-service/docker-compose.yml down -v
	podman-compose -f trade-service/docker-compose.yml down -v