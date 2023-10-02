import time
import speech_recognition as sr
import subprocess
from time import strftime
import datetime   #date and time for user
import pyttsx3 as tts   #text to speech package
#import wolframalpha as wolf
import json
import requests
#import wikipedia
import webbrowser
import os
from gtts import gTTS
import requests
from bs4 import BeautifulSoup
from googleapiclient.discovery import build #google email
from google.oauth2.credentials import Credentials #google email



# need to enable and sign up for Google API for email access

def listen_to_voice():
    recognizer = sr.Recognizer()
    with sr.Microphone() as source:
        speak_response("Listening...")
        audio = recognizer.listen(source)

    try:
        command = recognizer.recognize_google(audio)
        talk("You said: " + command) # type:ignore
        process_command(command)
    except sr.UnknownValueError:
        speak_response("Could not understand audio")
    except sr.RequestError as e:
        speak_response(f"Could not request results; {e}")

def speak_response(response):
    tts = gTTS(text=response, lang='en')
    tts.save('response.mp3')
    subprocess.run(['mpg123', 'response.mp3'])
    os.remove('response.mp3')

def process_command(command):
    if "call" in command:
        if "make" in command:
            make_phone_call(command)
        elif "reject" in command:
            reject_phone_call()
        elif "answer" in command:
            answer_phone_call()

    elif "text" in command:
        if "read text message" in command:
            messages = read_text_messages()
            if messages:
                speak_response("Here are your text messages:")
                for message in messages:
                    speak_response(message)
            else:
                speak_response("You have no text messages.")

    elif "send text message" in command:
        # Extract recipient and message from the command
        to = "recipient@example.com" #need to figure out how to input number here
        message = "This is a test message."
        send_text_message(to, message)
        speak_response("Message sent.")

    elif "read emails" in command:
        emails = read_emails() # type: ignore
        # Convert emails to speech and play them
        speak_response("Here are your emails: " + ', '.join(emails))
    
    elif "compose email" in command:
        # Extract recipient, subject, and message from the command
        to = "recipient@example.com"
        subject = "Test Subject"
        message = "This is a test email."
        compose_email(to, subject, message) # type: ignore
        speak_response("Email sent.")

    elif "search YouTube" in command:
        # Extract the search query from the command
        query = command.split("search YouTube for")[1].strip()
        search_youtube(query)

    elif "time" in command:
        time = datetime.datetime.now().strftime('%I:%M%p')
        speak_response("The current time is " + time)

    elif "date" in command:
        date = datetime.datetime.now().strftime('%m /%d /%Y')
        speak_response("Today's date is " + date)

    elif "Commands" in command:
        #list command promts for user as reminder of capabilities

    elif "set a timer" in command:
        # Extract the time duration from the command (e.g., "set a timer for 5 minutes")
        duration = extract_duration(command)
        if duration:
            set_timer(duration)

    elif "set an alarm for" in command:
        # Extract the alarm time from the command (e.g., "set an alarm for 8:30 AM")
        hour, minute = extract_alarm_time(command)
        if hour is not None and minute is not None:
            set_alarm(hour, minute)

    elif "search web for" in command:
        # Extract the search query from the command
        query = command.split("search for")[1].strip()
        web_search(query)

    elif "calculator" in command:
        # Extract the calculation command from the voice input (e.g., "calculate 5 plus 3")
        calculation_command = command.split("calculate")[1].strip()
        perform_calculation(calculation_command)

    elif "create calendar event" in command:
        # Extract the event name from the voice input
        event_name = command.split("create calendar event")[1].strip()
        create_calendar_event(event_name)

    else:
        print("Command not recognized")

def make_phone_call(phone_number):
    command = f'adb shell am start -a android.intent.action.CALL -d tel:{phone_number}'
    subprocess.run(command, shell=True)

def reject_phone_call():
    command = 'adb shell input keyevent 26'  # Keycode for the power button (screen off)
    subprocess.run(command, shell=True)
    time.sleep(1)  # Wait for the screen to turn off
    command = 'adb shell input keyevent 6'   # Keycode for the volume down button (reject call)
    subprocess.run(command, shell=True)

def answer_phone_call():
    command = 'adb shell input keyevent 79'  # Keycode for answering a call
    subprocess.run(command, shell=True)

def read_text_messages():
    command = 'adb shell content query --uri content://sms/inbox'
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    messages = result.stdout.split('\n')
    return messages

def send_text_message(phone_number, message):
    command = f'adb shell am start -a android.intent.action.SENDTO -d sms:{phone_number} --es sms_body "{message}"'
    subprocess.run(command, shell=True)

def authenticate_with_gmail_api():
    credentials = Credentials.from_authorized_user_file('path_to_credentials.json')
    service = build('gmail', 'v1', credentials=credentials)
    return service

def read_emails(service):
    try:
        messages = service.users().messages().list(userId='me', labelIds=['INBOX']).execute()
        message_list = messages.get('messages', [])
        if not message_list:
            response = "You have no emails."
        else:
            response = "Here are your emails:"
            for message in message_list:
                msg = service.users().messages().get(userId='me', id=message['id']).execute()
                response += f"Subject: {msg['subject']}. "
    except Exception as e:
        print(f"Error while reading emails: {str(e)}")
        response = "An error occurred while reading emails."

    speak_response(response)

def compose_email(service, to, subject, message):
    try:
        message = {
            'raw': f"From: me\r\nTo: {to}\r\nSubject: {subject}\r\n\r\n{message}"
        }
        service.users().messages().send(userId='me', body=message).execute()
        response = "Email sent."
    except Exception as e:
        print(f"Error while sending email: {str(e)}")
        response = "An error occurred while sending the email."

    speak_response(response)

def set_timer(seconds):
    command = f'adb shell "am start -a android.intent.action.SET_TIMER -e length {seconds}"'
    subprocess.run(command, shell=True)

def set_alarm(hour, minute):
    command = f'adb shell "am start -a android.intent.action.SET_ALARM --ei android.intent.extra.alarm.HOUR {hour} --ei android.intent.extra.alarm.MINUTES {minute}"'
    subprocess.run(command, shell=True)

def web_search(query):
    try:
        search_url = f"https://www.google.com/search?q={query}"
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(search_url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # Extract search results (e.g., titles and links)
        search_results = soup.find_all('div', class_='tF2Cxc')
        
        if search_results:
            # Limit the number of results to read
            num_results_to_read = min(5, len(search_results))
            response = "Here are some search results:"
            
            for i in range(num_results_to_read):
                result = search_results[i]
                title = result.h3.text
                link = result.a['href']
                response += f"{i + 1}. {title}. "
        else:
            response = "No search results found."
    except Exception as e:
        print(f"Error during web search: {str(e)}")
        response = "An error occurred during the web search."

    speak_response(response)

def search_youtube(query):
    try:
        # Launch the YouTube app using ADB
        subprocess.run(['adb', 'shell', 'am', 'start', '-n', 'com.google.android.youtube/.app.honeycomb.Shell$HomeActivity'])

        # Wait for the app to launch (you may need to adjust the delay)
        time.sleep(15)

        # Simulate typing the search query into the YouTube app
        subprocess.run(['adb', 'shell', 'input', 'text', query])

        # Simulate pressing the Enter key to perform the search
        subprocess.run(['adb', 'shell', 'input', 'keyevent', 'KEYCODE_ENTER'])

        # Wait for search results (you may need to adjust the delay)
        time.sleep(5)
        
        response = "YouTube search completed."
    except Exception as e:
        print(f"Error during YouTube search: {str(e)}")
        response = "An error occurred during the YouTube search."

    speak_response(response)

def perform_calculation(command):
    try:
        # Replace words with corresponding symbols
        command = command.replace("plus", "+").replace("minus", "-").replace("times", "*").replace("divided by", "/")

        # Evaluate the mathematical expression
        result = eval(command)
        response = f"The result is {result}"
    except Exception as e:
        print(f"Error during calculation: {str(e)}")
        response = "An error occurred during the calculation."

def create_calendar_event(event_name):
    try:
        # Launch the calendar app using ADB (replace with the actual package and activity)
        subprocess.run(['adb', 'shell', 'am', 'start', '-n', 'com.android.calendar/.CalendarActivity'])

        # Wait for the app to launch (you may need to adjust the delay)
        time.sleep(5)

        # Simulate typing the event name into the calendar app
        subprocess.run(['adb', 'shell', 'input', 'text', event_name])

        # Simulate pressing the Enter key to save the event
        subprocess.run(['adb', 'shell', 'input', 'keyevent', 'KEYCODE_ENTER'])

        response = "Calendar event created."
    except Exception as e:
        print(f"Error creating calendar event: {str(e)}")
        response = "An error occurred while creating the calendar event."

    speak_response(response)

if __name__ == "__main__":
    while True:
        listen_to_voice()
