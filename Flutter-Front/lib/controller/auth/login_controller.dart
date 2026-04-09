import 'dart:convert';

import 'package:busanit501_flutter_workspace_251021/screen/main_screen.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;


class LoginController extends ChangeNotifier {
  final TextEditingController idController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  final FlutterSecureStorage secureStorage = const FlutterSecureStorage(); // 보안 저장소
  // final String serverIp = "http://192.168.219.103:8080"; // 서버 주소
  // 주의사항, 각자 아이피를 사용해야합니다. 그대로 사용하면 안됩니다.
  // localhost 로 하면 안됩니다.
  // 만약, 에뮬레이터로 진행을 하면, : 10.0.2.2 로 변경해서 진행해보기.
  final String serverIp = "http://10.0.2.2:8080"; // 서버 주소 변경 필요
  bool isLoading = false; // 로그인 로딩 상태
  bool isLoggedIn = false; // 로그인 여부

  LoginController() {
    _checkLoginStatus(); // 생성 시 로그인 상태 확인
  }

  // 로그인 요청 및 JWT 저장
  Future<void> login(BuildContext context) async {
    String inputId = idController.text.trim();
    String inputPw = passwordController.text.trim();

    if (inputId.isEmpty || inputPw.isEmpty) {
      _showDialog(context, "오류", "아이디와 비밀번호를 입력하세요.");
      return;
    }

    Map<String, String> loginData = {"mid": inputId, "mpw": inputPw};

    try {
      isLoading = true;
      notifyListeners(); // UI 업데이트

      final response = await http.post(
        Uri.parse("$serverIp/generateToken"),
        headers: {"Content-Type": "application/json"},
        body: jsonEncode(loginData),
      );

      if (response.statusCode == 200) {
        final responseData = jsonDecode(response.body);

        String accessToken = responseData["accessToken"];
        String refreshToken = responseData["refreshToken"];
        String profileImg = responseData["profileImg"] ?? "";

        // JWT 토큰 보안 저장소에 저장
        await secureStorage.write(key: "accessToken", value: accessToken);
        await secureStorage.write(key: "refreshToken", value: refreshToken);
        await secureStorage.write(key: "profileImg", value: profileImg);
        await secureStorage.write(key: "mid", value: inputId);

        // 도서관 회원 numeric ID 조회 후 저장
        try {
          final memberRes = await http.get(
            Uri.parse("$serverIp/api/member/me?mid=$inputId"),
            headers: {"Authorization": "Bearer $accessToken"},
          );
          if (memberRes.statusCode == 200) {
            final memberData = jsonDecode(memberRes.body);
            await secureStorage.write(
                key: "memberId", value: memberData["id"].toString());
          }
        } catch (_) {}

        clearInputFields();

        isLoggedIn = true;
        notifyListeners();

        if (!context.mounted) return;
        _showDialog(context, "로그인 성공", "메인 화면으로 이동합니다.");

        Future.delayed(const Duration(seconds: 1), () {
          if (!context.mounted) return;
          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(builder: (context) => const MainScreen2()),
            (Route<dynamic> route) => false,
          );
        });
      } else {
        if (!context.mounted) return;
        _showDialog(context, "로그인 실패", "아이디 또는 비밀번호가 잘못되었습니다.");
      }
    } catch (e) {
      if (!context.mounted) return;
      _showDialog(context, "네트워크 오류", "오류 발생: $e");
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
// 로그아웃 확인 다이얼로그
  Future<void> showLogoutDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("로그아웃 확인"),
          content: const Text("정말 로그아웃 하시겠습니까?"),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(), // 취소 버튼
              child: const Text("취소"),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // 다이얼로그 닫기
                logout(context); // 로그아웃 실행
              },
              child: const Text("확인", style: TextStyle(color: Colors.red)),
            ),
          ],
        );
      },
    );
  }

  // 로그아웃 기능 (저장된 로그인 정보 삭제)
  Future<void> logout(BuildContext context) async {
    await secureStorage.delete(key: "accessToken");
    await secureStorage.delete(key: "refreshToken");
    await secureStorage.delete(key: "profileImg");
    await secureStorage.delete(key: "mid");
    await secureStorage.delete(key: "memberId");

    isLoggedIn = false;
    notifyListeners();

    if (!context.mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (context) => const MainScreen2()),
      (Route<dynamic> route) => false,
    );
  }

  // 입력 필드 초기화
  void clearInputFields() {
    idController.clear();
    passwordController.clear();
    notifyListeners();
  }

  // 로그인 상태 확인 (앱 실행 시 호출)
  Future<void> _checkLoginStatus() async {
    String? mid = await secureStorage.read(key: "mid");
    isLoggedIn = mid != null;
    notifyListeners();
  }

  // 보안 저장소에서 JWT 토큰 가져오기
  Future<String?> getAccessToken() async {
    return await secureStorage.read(key: "accessToken");
  }

  // 보안 저장소에서 로그인한 사용자 ID 가져오기
  Future<String?> getUserId() async {
    return await secureStorage.read(key: "mid"); // 저장된 ID 불러오기
  }

  // 다이얼로그 표시 함수
  void _showDialog(BuildContext context, String title, String message) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text("확인"),
            ),
          ],
        );
      },
    );
  }
  // ✅ --- dispose 함수 추가 ---
  @override
  void dispose() {
    idController.dispose();
    passwordController.dispose();
    super.dispose();
  }
}