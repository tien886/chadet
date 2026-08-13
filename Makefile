.PHONY: web dev-all infra-all down infra-auth infra-chat infra-trade infra-rabbitmq dev-auth dev-chat dev-trade dev-gateway

infra-auth:
	podman-compose -f auth-service/docker-compose.yml up -d

infra-chat:
	podman-compose -f chat-service/docker-compose.yml up -d

infra-trade:
	podman-compose -f trade-service/docker-compose.yml up -d

infra-rabbitmq:
	podman-compose -f docker-compose.yml up -d rabbitmq

dev-auth:
	mvn clean spring-boot:run -pl auth-service 

dev-chat:
	mvn clean spring-boot:run -pl chat-service 

dev-trade:
	mvn clean spring-boot:run -pl trade-service

dev-gateway:
	mvn clean spring-boot:run -pl gateway-service 

web:
	python3 -m http.server 3000 --directory web 

infra-all:
	make infra-rabbitmq;
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
	podman-compose -f docker-compose.yml down -v
	podman-compose -f auth-service/docker-compose.yml down -v
	podman-compose -f chat-service/docker-compose.yml down -v
	podman-compose -f trade-service/docker-compose.yml down -v