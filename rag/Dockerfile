FROM python:3.11-slim

WORKDIR /app

# We need gcc installed for some python libraries as they have C bindings
RUN apt-get update && apt-get install gcc -y && apt-get install g++ -y && apt-get install python3-dev -y && apt-get clean

# Copy the requirements file into the container at /app
COPY ./rag/requirements.txt .
# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy the entire source code into the container at /app
COPY ./rag/src/ .

# Expose the port that the Flask app runs on
EXPOSE 5678

# Define the command to run your Flask application
CMD ["python", "webserver.py"]
