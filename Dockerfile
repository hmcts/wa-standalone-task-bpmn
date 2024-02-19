ARG APP_INSIGHTS_AGENT_VERSION=3.4.8

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/wa-standalone-task-bpmn.jar /opt/app/

EXPOSE 4550
CMD [ "wa-standalone-task-bpmn.jar" ]
