# Web Server Read-Me

This is a basic Java web server that can handle GET and POST requests and execute PHP scripts. Follow these instructions to run the server:

## Prerequisites
- Java Development Kit (JDK) installed
- PHP installed and added to the system's PATH (for executing PHP scripts)

## Running the Server
1. Compile the `Main.java` file:

2. Start the server by running the `Main` class:

3. The server will start and listen on port 2728. You can change the port by modifying the `port` variable in the `Main` class.

## Sending Requests
- To send a GET request, open a web browser and navigate to `http://localhost:2728/` followed by the desired file path (e.g., `http://localhost:2728/index.html`).

- To send a POST request, you can use tools like cURL or create a web form that submits data to the server. The server handles POST requests and executes PHP scripts.

## Directory Structure
- Static HTML and PHP files should be placed in the `htdocs` directory.

## Notes
- Ensure that the PHP interpreter is correctly installed and available in the system's PATH for executing PHP scripts.

- Customize the server's response headers and error handling as needed for your application.

- Handle PHP scripts and POST data according to your specific requirements within the provided code.

- Make sure to secure your server and sanitize user inputs to prevent security vulnerabilities.
