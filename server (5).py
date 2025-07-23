from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from datetime import datetime
import os
import json

app = Flask(__name__)
CORS(app)  # Добавляем поддержку CORS для Android приложения
messages = []

@app.route('/send', methods=['POST'])
def send_message():
    try:
        data = request.get_json()

        # Проверка обязательных полей
        if not all(k in data for k in ['from', 'to', 'text']):
            return "Missing fields", 400

        # Добавим дату, если не пришла с клиента
        if 'date' not in data:
            data['date'] = datetime.now().strftime('%d.%m.%Y %H:%M')

        # Добавляем статус прочтения
        data['read'] = False

        messages.append(data)
        return '', 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/messages', methods=['GET'])
def get_messages():
    try:
        user1 = request.args.get('from')
        user2 = request.args.get('to')

        # Если не указаны участники, вернуть все
        if not user1 or not user2:
            return jsonify(messages)

        # Фильтрация: только сообщения между двумя участниками
        filtered = [
            m for m in messages
            if (m['from'] == user1 and m['to'] == user2)
            or (m['from'] == user2 and m['to'] == user1)
        ]
        return jsonify(filtered)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/mark_read', methods=['POST'])
def mark_read():
    try:
        data = request.get_json()
        from_user = data.get('from')
        to_user = data.get('to')
        count = 0
        for m in messages:
            if m['from'] == from_user and m['to'] == to_user and not m.get('read', False):
                m['read'] = True
                count += 1
        return jsonify({'marked': count})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/clear', methods=['POST'])
def clear_messages():
    try:
        global messages
        messages = []
        return 'Очищено', 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/send_media', methods=['POST'])
def send_media():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file provided"}), 400
            
        file = request.files['file']
        from_user = request.form['from']
        to_user = request.form['to']
        
        if not file or file.filename == '':
            return jsonify({"error": "No file selected"}), 400
            
        ext = file.filename.split('.')[-1].lower()
        filetype = 'image' if ext in ['jpg', 'jpeg', 'png', 'gif'] else 'video'
        filename = f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{file.filename}"
        filepath = os.path.join(MEDIA_FOLDER, filename)
        file.save(filepath)
        media_url = f"http://{request.host}/media/{filename}"
        message = {
            "from": from_user,
            "to": to_user,
            "type": filetype,
            "mediaUrl": media_url,
            "date": datetime.now().strftime('%d.%m.%Y %H:%M'),
            "read": False
        }
        messages.append(message)
        return jsonify(message)  # Return the full message object
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/media/<filename>')
def serve_media(filename):
    try:
        return send_from_directory(MEDIA_FOLDER, filename)
    except Exception as e:
        return jsonify({"error": str(e)}), 404

@app.route("/register", methods=["POST"])
def register_user():
    try:
        data = request.json
        username = data.get("username", "").strip()

        if not username:
            return jsonify({"success": False, "error": "Имя пустое"}), 400

        # Ensure users.json exists
        if not os.path.exists("users.json"):
            with open("users.json", "w") as f:
                json.dump([], f)

        with open("users.json", "r") as f:
            users = json.load(f)

        if username in users:
            return jsonify({"success": False, "error": "Имя занято"}), 409

        users.append(username)
        with open("users.json", "w") as f:
            json.dump(users, f)

        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

MEDIA_FOLDER = 'media'
os.makedirs(MEDIA_FOLDER, exist_ok=True)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
