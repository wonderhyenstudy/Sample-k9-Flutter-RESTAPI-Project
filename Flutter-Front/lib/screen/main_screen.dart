import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:provider/provider.dart';

import '../controller/auth/login_controller.dart';

class MainScreen2 extends StatefulWidget {
  const MainScreen2({super.key});

  @override
  State<MainScreen2> createState() => _MainScreen2State();
}

class _MainScreen2State extends State<MainScreen2> {
  // 보안 저장소에 저장된, 로그인 된 유저 정보를 가져오기 준비 작업.
  final FlutterSecureStorage secureStorage = const FlutterSecureStorage();

  // ✅ 서버 IP (SignupController와 동일하게 설정해야 합니다)
  // 10.0.2.2 ,에뮬레이터 인경우
  final String serverIp = "http://10.0.2.2:8080"; // ‼️ localhost 대신 실제 IP 사용

  String? userId;
  String? profileImgId; // ✅ 프로필 이미지 ID를 저장할 변수

  @override
  void initState() {
    super.initState();
    // ✅ 함수 이름 변경
    _loadUserData();
  }

  // ✅ 보안 저장소에서 로그인한 유저 ID 및 프로필 이미지 ID 불러오기
  Future<void> _loadUserData() async {
    // 저장된 키 이름이 "profileImgId"가 맞는지 확인 필요 (로그인/회원가입 시 저장한 키)
    String? mid = await secureStorage.read(key: "mid");
    String? imgId = await secureStorage.read(key: "profileImg"); // ✅ 이미지 ID 가져오기

    setState(() {
      userId = mid;
      profileImgId = imgId; // ✅ 상태 변수에 저장
    });
  }

  @override
  Widget build(BuildContext context) {
    final loginController = context.watch<LoginController>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('메인화면'),
        actions: [
          // 로그인 상태 일때만, 로그아웃 버튼 표시
          if (loginController.isLoggedIn)
            IconButton(
                onPressed: () => loginController.showLogoutDialog(context),
                icon: const Icon(Icons.logout))
        ],
      ),
      body: SafeArea(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // 프로필 이미지
              Center(
                child: CircleAvatar(
                  radius: 60,
                  backgroundColor: Colors.grey[200],
                  backgroundImage: (profileImgId != null && profileImgId!.isNotEmpty)
                      ? NetworkImage("$serverIp/member/view/$profileImgId")
                      : null,
                  onBackgroundImageError: (profileImgId != null && profileImgId!.isNotEmpty)
                      ? (exception, stackTrace) {
                          print("프로필 이미지 로드 오류: $exception");
                        }
                      : null,
                  child: (profileImgId == null || profileImgId!.isEmpty)
                      ? Icon(Icons.person, size: 60, color: Colors.grey[600])
                      : null,
                ),
              ),
              const SizedBox(height: 24),

              // 로그인 상태 텍스트
              Center(
                child: Text(
                  userId != null ? "환영합니다, $userId님!" : "로그인이 필요합니다.",
                  style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
              ),
              const SizedBox(height: 16),

              // 비로그인 상태: 로그인 / 회원가입 버튼
              if (!loginController.isLoggedIn) ...[
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/login'),
                  child: const Text('로그인'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/signup'),
                  child: const Text('회원 가입'),
                ),
              ],

              // 로그인 상태: 도서관 메뉴
              if (loginController.isLoggedIn) ...[
                const Divider(),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 8),
                  child: Text('도서관 서비스', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/bookList'),
                  child: const Text('도서 검색'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/noticeList'),
                  child: const Text('공지사항'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/eventList'),
                  child: const Text('도서관 행사'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/facilityReserve'),
                  child: const Text('시설 예약'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/mypage'),
                  child: const Text('마이페이지'),
                ),
                const Divider(),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/todos'),
                  child: const Text('Todos 일정'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/ai-image'),
                  child: const Text('AI 이미지'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/ai-stock'),
                  child: const Text('AI 주가'),
                ),
              ],
            ],
          )),
    );
  }
}
