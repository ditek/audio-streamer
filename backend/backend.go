package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

/************* Websocket **************/

const maxMessageSize = 4096 * 8

var upgrader = websocket.Upgrader{
	ReadBufferSize:  maxMessageSize,
	WriteBufferSize: maxMessageSize,
}

/*****************************************/

const audioBufferSize = 16

var audioBuffer chan []byte

func echoHandler(w http.ResponseWriter, r *http.Request) {
	conn, _ := upgrader.Upgrade(w, r, nil)

	for {
		// Read message from browser
		msgType, msg, err := conn.ReadMessage()
		if err != nil {
			fmt.Println(err.Error())
			return
		}

		fmt.Printf("%s received: %s\n", conn.RemoteAddr(), string(msg))

		// Write message back to browser
		if err = conn.WriteMessage(msgType, msg); err != nil {
			return
		}
	}
}

func streamHandler(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("Error %v", err)
		return
	}

	log.Println("Connection with Android established")
	dots := ""

	for {
		// Read message from browser
		_, msg, err := conn.ReadMessage()
		if err != nil {
			fmt.Println(err.Error())
			return
		}

		// Print the message to the console
		fmt.Printf("received msg with len %d. Buffer has %d msgs %s\n",
			len(msg), len(audioBuffer), dots)
		dots += "."
		if len(dots) == 60 {
			dots = ""
		}

		// Make sure we don't get stuck if the buffer is not being read
		if len(audioBuffer) >= audioBufferSize {
			<-audioBuffer
		}
		audioBuffer <- msg
	}
}

func frontendHandler(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("Error %v", err)
		return
	}

	log.Println("Connection with frontend established")

	for {
		if err = conn.WriteMessage(websocket.BinaryMessage, <-audioBuffer); err != nil {
			log.Printf("Err Sending Websocket to browser: %v", err)
			return
		}
	}
}

func main() {
	audioBuffer = make(chan []byte, audioBufferSize)

	// Echo endpoint for testing
	http.HandleFunc("/echo", echoHandler)

	// Receive data from Android
	http.HandleFunc("/stream", streamHandler)

	// Send binary data to browser
	http.HandleFunc("/ws", frontendHandler)

	// Frontend homepage
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "./frontend")
	})

	// err := http.ListenAndServe("10.10.48.147:80", nil)
	// err := http.ListenAndServe("192.168.1.208:80", nil)
	err := http.ListenAndServe(":80", nil)
	if err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}
