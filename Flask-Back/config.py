import os
from dotenv import load_dotenv

# .env 환경 변수 로드
load_dotenv()

# --- AWS S3 설정 ---
AWS_BUCKET = os.getenv("AWS_STORAGE_BUCKET_NAME")
AWS_ACCESS_KEY_ID = os.getenv("AWS_S3_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_S3_SECRET_ACCESS_KEY")
AWS_REGION = os.getenv("AWS_S3_REGION_NAME")

# --- 기본 경로 설정 ---
import torch

BASE_DIR = os.path.dirname(__file__)
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'uploads')
RESULT_FOLDER = os.path.join(BASE_DIR, 'results')
TEMPLATE_FOLDER = os.path.join(BASE_DIR, 'templates')

# --- 하드웨어 설정 ---
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# --- 모델 및 데이터 파일 경로 설정 ---
# 1. 이미지 분류 모델 (ResNet)
VISION_MODEL_CONFIGS = {
    "animal": {
        "model_path": os.path.join(BASE_DIR, "resnet50_best_team1_animal.pth"),
        "num_classes": 5,
        "class_labels": ["고양이", "공룡", "강아지", "꼬북이", "티벳여우"],
    },
    "appliance": {
        "model_path": os.path.join(BASE_DIR, "resnet50_best_team2_recycle.pth"),
        "num_classes": 13,
        "class_labels": ["영업용_냉장고", "컴퓨터_cpu", "드럼_세탁기", "냉장고", "컴퓨터_그래픽카드", "메인보드", "전자레인지", "컴퓨터_파워", "컴퓨터_램",
                         "스탠드_에어컨", "TV", "벽걸이_에어컨", "통돌이_세탁기"],
    },
    "tool": {
        "model_path": os.path.join(BASE_DIR, "resnet50_best_team3_tools_accuracy_90.pth"),
        "num_classes": 10,
        "class_labels": ["공구 톱", "공업용가위", "그라인더", "니퍼", "드라이버", "망치", "스패너", "전동드릴", "줄자", "캘리퍼스"],
    },
}

# 2. 객체 탐지 모델 (YOLO)
YOLO_MODEL_PATH = os.path.join(BASE_DIR, "best-busanit501-aqua.pt")

# 3. 주가 예측 모델 (RNN, LSTM, GRU)
STOCK_MODEL_PATHS = {
    'RNN': {
        'model': os.path.join(BASE_DIR, 'Rnn-samsungStock.pth'),
        'scaler': os.path.join(BASE_DIR, 'Rnn-scaler.pth')
    },
    'LSTM': {
        'model': os.path.join(BASE_DIR, 'samsungStock_LSTM_60days_basic.pth'),
        'scaler': os.path.join(BASE_DIR, 'scaler_LSTM_60days_basic.pth')
    },
    'GRU': {
        'model': os.path.join(BASE_DIR, 'samsungStock_GRU.pth'),
        'scaler': os.path.join(BASE_DIR, 'scaler_GRU.pth')
    }
}

# 4. 주가 데이터
STOCK_CSV_PATH = os.path.join(BASE_DIR, '7-samsung_stock_2022_01_2025_10_13.csv')
