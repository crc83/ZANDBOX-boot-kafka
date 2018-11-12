package com.sbelei.kafka.kafkademo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

@SpringBootApplication
public class KafkademoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(KafkademoApplication.class, args);
	}
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	 
	public void send(String topic, String payload) {
	    kafkaTemplate.send(topic, payload);
	    System.out.println("Message: "+payload+" sent to topic: "+topic);
	}
	
	@KafkaListener(topics = "topic1")
	public void receiveTopic1(ConsumerRecord<?, ?> consumerRecord) {
	    System.out.println("Receiver on topic1: "+consumerRecord.toString());
	}

	@Override
	public void run(String... args) throws Exception {
		for (int i = 0; i < 100; i++) {
			send("topic1", "msg"+i);
		}
		
	}
}
