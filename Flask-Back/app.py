# app.py (통합 및 디버깅 강화 버전)

import os
import io
import uuid
import threading
import traceback  # 👈 상세 에러 출력을 위해 추가

import torch
import torch.nn as nn
import torchvision.transforms as transforms
from torchvision import models
from flask import Flask, request, jsonify, render_template, url_for, send_from_directory
from flask_cors import CORS
from PIL import Image
import cv2
from werkzeug.utils import secure_filename
from ultralytics import YOLO
import numpy as np
import pandas as pd

import config  # 설정 파일 임포트

# ==============================================================================
# 1. Flask 앱 및 기본 설정
# ==============================================================================
app = Flask(__name__)
# CORS 활성화: 모바일 기기(Flutter) 및 외부 백엔드(Spring Boot) 연동 허용
CORS(app)

device = config.DEVICE
print(f"✅ Using device: {device}")

UPLOAD_FOLDER = config.UPLOAD_FOLDER
RESULT_FOLDER = config.RESULT_FOLDER
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(RESULT_FOLDER, exist_ok=True)


# ==============================================================================
# 2. 주가 예측 모델 클래스 정의
# ==============================================================================
class StockPredictorRNN(nn.Module):
    def __init__(self, input_size=4, hidden_size=64, num_layers=1, output_size=1):
        super(StockPredictorRNN, self).__init__()
        self.hidden_size = hidden_size  # 👈 이 줄을 추가하세요.
        self.num_layers = num_layers  # 👈 이 줄을 추가하세요.
        self.rnn = nn.RNN(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(x.device)
        out, _ = self.rnn(x, h0)
        return self.fc(out[:, -1, :])


class LSTMModel(nn.Module):
    def __init__(self, input_size=4, hidden_size=128, output_size=1, num_layers=2, dropout=0.3):
        super(LSTMModel, self).__init__()
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True, dropout=dropout)
        self.fc = nn.Linear(hidden_size, output_size)
        self.relu = nn.ReLU()

    def forward(self, x):
        lstm_out, _ = self.lstm(x)
        last_out = lstm_out[:, -1, :]
        return self.fc(self.relu(last_out))


class GRUModel(nn.Module):
    def __init__(self, input_size=4, hidden_size=64, num_layers=1, output_size=1):
        super(GRUModel, self).__init__()
        self.gru = nn.GRU(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        out, _ = self.gru(x)
        return self.fc(out[:, -1])


# ==============================================================================
# 3. 모든 모델 및 데이터 로딩
# ==============================================================================

# --- 이미지 분류 모델 설정 (요청 시 로드) ---
# 설정된 config.py 파일의 통합 구조 호출
MODEL_CONFIGS = config.VISION_MODEL_CONFIGS

# --- YOLO 모델 로드 (시작 시 로드) ---
try:
    yolo_model = YOLO(config.YOLO_MODEL_PATH)
    print("✅ YOLO model loaded successfully.")
except Exception as e:
    print(f"🔴 ERROR loading YOLO model: {e}")
    yolo_model = None

# --- 주가 예측 모델 및 데이터 로드 (시작 시 로드) ---
stock_models = {}
stock_scalers = {}

try:
    stock_models['RNN'] = StockPredictorRNN().to(device)
    stock_models['RNN'].load_state_dict(torch.load(config.STOCK_MODEL_PATHS['RNN']['model'], map_location=device))
    stock_models['RNN'].eval()
    stock_scalers['RNN'] = torch.load(config.STOCK_MODEL_PATHS['RNN']['scaler'], map_location=device)

    stock_models['LSTM'] = LSTMModel().to(device)
    stock_models['LSTM'].load_state_dict(torch.load(config.STOCK_MODEL_PATHS['LSTM']['model'], map_location=device))
    stock_models['LSTM'].eval()
    stock_scalers['LSTM'] = torch.load(config.STOCK_MODEL_PATHS['LSTM']['scaler'], map_location=device)

    stock_models['GRU'] = GRUModel().to(device)
    stock_models['GRU'].load_state_dict(torch.load(config.STOCK_MODEL_PATHS['GRU']['model'], map_location=device))
    stock_models['GRU'].eval()
    stock_scalers['GRU'] = torch.load(config.STOCK_MODEL_PATHS['GRU']['scaler'], map_location=device)
    print("✅ Stock prediction models and scalers loaded successfully.")
except FileNotFoundError as e:
    print(f"🔴 ERROR: Stock model or scaler file not found: {e.filename}")
except Exception as e:
    print(f"🔴 ERROR loading stock models: {e}")

try:
    stock_df = pd.read_csv(config.STOCK_CSV_PATH, index_col='Date', parse_dates=True)
    stock_df.sort_index(inplace=True)
    print("✅ Stock data CSV loaded successfully.")
except FileNotFoundError:
    print("🔴 ERROR: Stock data CSV not found.")
    stock_df = None


# ==============================================================================
# 4. 헬퍼 함수 (YOLO 처리, 모델 로드)
# ==============================================================================

def process_yolo(file_path, output_path, file_type):
    # ... (기존 코드와 동일, 생략) ...
    try:
        if file_type == 'image':
            results = yolo_model(file_path)
            result_img = results[0].plot()
            cv2.imwrite(output_path, result_img)

        elif file_type == 'video':
            temp_output_path = output_path + ".tmp"
            cap = cv2.VideoCapture(file_path)
            if not cap.isOpened():
                print(f"🔴 ERROR: 비디오 파일을 열 수 없습니다: {file_path}")
                return

            width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            fps = int(cap.get(cv2.CAP_PROP_FPS))

            if fps <= 0:
                fps = 30
                print(f"⚠️ WARNING: FPS를 감지할 수 없어 기본값 {fps}로 설정합니다.")

            codecs_to_try = [('mp4v', '.mp4'), ('XVID', '.avi'), ('MJPG', '.avi')]
            out = None
            for codec, ext in codecs_to_try:
                try:
                    fourcc = cv2.VideoWriter_fourcc(*codec)
                    if not temp_output_path.endswith(ext):
                        temp_output_path = os.path.splitext(temp_output_path)[0] + ext
                        output_path = os.path.splitext(output_path)[0] + ext
                    out = cv2.VideoWriter(temp_output_path, fourcc, fps, (width, height))
                    if out.isOpened():
                        print(f"✅ VideoWriter 초기화 성공: 코덱={codec}, 해상도={width}x{height}, FPS={fps}")
                        break
                    else:
                        out.release()
                        out = None
                except Exception as e:
                    print(f"⚠️ {codec} 코덱 오류: {e}")
                    continue

            if out is None or not out.isOpened():
                print(f"🔴 ERROR: 모든 코덱 시도 실패. OpenCV 비디오 출력을 초기화할 수 없습니다.")
                cap.release()
                return

            while cap.isOpened():
                ret, frame = cap.read()
                if not ret: break
                results = yolo_model(frame)
                result_frame = results[0].plot()
                out.write(result_frame)

            cap.release()
            out.release()

            if os.path.exists(temp_output_path):
                os.rename(temp_output_path, output_path)
                print(f"✅ YOLO 처리 완료: {output_path}")
            else:
                print(f"🔴 ERROR: 임시 파일({temp_output_path})이 생성되지 않았습니다.")
    except Exception as e:
        print(f"🔴 ERROR in process_yolo thread: {e}")


def load_classification_model(model_type):
    config = MODEL_CONFIGS[model_type]
    model = models.resnet50(pretrained=False)
    num_ftrs = model.fc.in_features
    model.fc = nn.Linear(num_ftrs, config["num_classes"])
    model.load_state_dict(torch.load(config["model_path"], map_location=device))
    model.to(device)
    model.eval()
    return model, config["class_labels"]


# ==============================================================================
# 5. Flask 라우트 (API 엔드포인트)
# ==============================================================================

@app.route("/")
def index():
    return render_template('index.html')


# --- 프론트엔드 연동용 호환 AI 분류 처리 엔드포인트 ---
# Flutter 앱 및 Spring 백엔드에서 '/predict/animal', '/predict/tool', '/predict/appliance' 형태로 요청 시 처리.
@app.route("/predict/<model_type>", methods=["POST"])
def predict(model_type):
    if "image" not in request.files:
        return jsonify({"error": "이미지가 업로드되지 않았습니다."}), 400
    file = request.files["image"]
    if file.filename == "":
        return jsonify({"error": "파일이 선택되지 않았습니다."}), 400

    if model_type == 'yolo':
        # ... (YOLO 처리 로직, 기존과 동일) ...
        original_filename = file.filename
        filename_base, file_extension = os.path.splitext(original_filename)

        if file_extension.lower() in ['.jpg', '.jpeg', '.png', '.bmp']:
            file_type = 'image'
        elif file_extension.lower() in ['.mp4', '.avi', '.mov', '.mkv']:
            file_type = 'video'
        else:
            return jsonify({"error": "지원되지 않는 파일 형식입니다."}), 400

        safe_filename_base = secure_filename(filename_base)
        unique_id = str(uuid.uuid4())[:8]
        safe_filename = f"{safe_filename_base}_{unique_id}{file_extension}"
        file_path = os.path.join(UPLOAD_FOLDER, safe_filename)
        file.save(file_path)

        if file_type == 'video':
            output_filename = f"result_{safe_filename_base}_{unique_id}.mp4"
        else:
            output_filename = f"result_{safe_filename}"
        output_path = os.path.join(RESULT_FOLDER, output_filename)

        thread = threading.Thread(target=process_yolo, args=(file_path, output_path, file_type))
        thread.start()

        return jsonify({
            "message": "YOLO 처리가 시작되었습니다.",
            "output_filename": output_filename,
            "result_url": request.host_url.rstrip('/') + url_for('serve_result', filename=output_filename),
            "status_url": request.host_url.rstrip('/') + url_for('get_status', filename=output_filename)
        })

    elif model_type in MODEL_CONFIGS:
        try:
            model, class_labels = load_classification_model(model_type)
            image_bytes = file.read()
            image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

            transform = transforms.Compose([
                transforms.Resize((224, 224)),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
            ])
            image_tensor = transform(image).unsqueeze(0).to(device)

            with torch.no_grad():
                outputs = model(image_tensor)
                probabilities = torch.nn.functional.softmax(outputs[0], dim=0)
                predicted_idx = torch.argmax(probabilities).item()
                confidence = probabilities[predicted_idx].item() * 100

            # 메모리 해제 (중요)
            del model, image_tensor, outputs
            if torch.cuda.is_available():
                torch.cuda.empty_cache()

            return jsonify({
                "filename": file.filename,
                "predicted_class": class_labels[predicted_idx],
                "confidence": f"{confidence:.2f}%",
            })
        except Exception as e:
            # 👈 에러 발생 시 터미널에 상세 로그 출력
            print(f"🔴 ERROR in /predict/{model_type}: {e}")
            traceback.print_exc()
            return jsonify({"error": "Internal server error during prediction.", "details": str(e)}), 500
    else:
        return jsonify({"error": f"지원되지 않는 모델 유형: {model_type}"}), 400


@app.route('/status/<filename>')
def get_status(filename):
    if os.path.exists(os.path.join(RESULT_FOLDER, filename)):
        return jsonify({"status": "complete", "url": url_for('serve_result', filename=filename)})
    return jsonify({"status": "processing"})


@app.route('/results/<filename>')
def serve_result(filename):
    return send_from_directory(RESULT_FOLDER, filename)


# --- 주가 예측 API ---
@app.route('/api/stockdata')
def get_stockdata():
    if stock_df is None:
        return jsonify({"error": "서버에 데이터 파일이 없습니다."}), 500
    # ... (기존 코드와 동일, 생략) ...
    period = request.args.get('period', '5d')
    df_copy = stock_df.copy()
    df_copy.columns = [col.capitalize() for col in df_copy.columns]
    period_map = {'1d': 1, '5d': 5}
    days = period_map.get(period, 5)
    recent_data = df_copy.tail(days)

    if period == '1d':
        data_subset = recent_data
    else:
        data_subset = recent_data.iloc[:-1]

    data_subset = data_subset.reset_index()
    data_subset['Date'] = data_subset['Date'].dt.strftime('%Y-%m-%d')
    return jsonify(data_subset.to_dict(orient='records'))


@app.route('/api/predict2/<string:model_type>', methods=['POST'])
def predict2(model_type):
    try:
        req_data = request.get_json()
        input_data = req_data.get('data')
        period = req_data.get('period')
        model_key = model_type.upper()

        model = stock_models.get(model_key)
        scaler = stock_scalers.get(model_key)

        if not model or not scaler:
            return jsonify({"error": f"'{model_type}' 모델을 서버에서 찾을 수 없습니다."}), 404

        # ... (데이터 검증 및 예측 로직, 기존과 동일) ...
        input_np = np.array(input_data)
        input_scaled = scaler.transform(input_np)
        input_tensor = torch.Tensor(input_scaled).unsqueeze(0).to(device)

        with torch.no_grad():
            prediction_scaled = model(input_tensor).item()

        prediction = scaler.inverse_transform([[0, 0, 0, prediction_scaled]])[0][3]
        return jsonify({"prediction": round(prediction, 2)})

    except Exception as e:
        print(f"🔴 ERROR in /api/predict2/{model_type}: {e}")
        traceback.print_exc()
        return jsonify({"error": "예측 중 서버 오류가 발생했습니다.", "details": str(e)}), 500


# ==============================================================================
# 6. Flask 앱 실행
# 플러터 연결시,
# pip install waitress
# ==============================================================================
if __name__ == '__main__':
    from waitress import serve
    print("🚀 Waitress 서버 시작: http://0.0.0.0:5000")
    serve(app, host='0.0.0.0', port=5000, threads=4)