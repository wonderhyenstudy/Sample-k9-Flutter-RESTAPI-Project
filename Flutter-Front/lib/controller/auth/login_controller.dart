import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

class LoginController extends ChangeNotifier {
  final TextEditingController idController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  final FlutterSecureStorage secureStorage = const FlutterSecureStorage();
  final String serverIp = 'http://10.0.2.2:8080';

  bool isLoading = false;
  bool isLoggedIn = false;

  // 도서관 회원 정보 (GET /api/member/me 응답)
  bool isMemberInfoLoading = false;
  String? memberName;
  String? memberEmail;
  String? memberRegion;
  String? memberRole;
  String? memberRegDate;
  String? currentMid; // SecureStorage에서 읽은 mid

  LoginController() {
    _checkLoginStatus();
  }

  // ── 로그인 ────────────────────────────────────────────
  Future<void> login(BuildContext context) async {
    final inputId = idController.text.trim();
    final inputPw = passwordController.text.trim();

    if (inputId.isEmpty || inputPw.isEmpty) {
      _showDialog(context, '오류', '아이디와 비밀번호를 입력하세요.');
      return;
    }

    isLoading = true;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('$serverIp/generateToken'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'mid': inputId, 'mpw': inputPw}),
      );

      if (!context.mounted) return;

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final accessToken = data['accessToken'] as String;
        final refreshToken = data['refreshToken'] as String;
        final profileImg = data['profileImg'] ?? '';

        await secureStorage.write(key: 'accessToken', value: accessToken);
        await secureStorage.write(key: 'refreshToken', value: refreshToken);
        await secureStorage.write(key: 'profileImg', value: profileImg);
        await secureStorage.write(key: 'mid', value: inputId);
        currentMid = inputId;

        // 도서관 회원 ID + 상세 정보 조회
        try {
          final memberRes = await http.get(
            Uri.parse('$serverIp/api/member/me?mid=$inputId'),
            headers: {'Authorization': 'Bearer $accessToken'},
          );
          if (memberRes.statusCode == 200) {
            final md = jsonDecode(memberRes.body);
            await secureStorage.write(
                key: 'memberId', value: md['id'].toString());
            _applyMemberData(md);
          }
        } catch (_) {}

        clearInputFields();
        isLoggedIn = true;
        notifyListeners();

        if (!context.mounted) return;
        Navigator.pushNamedAndRemoveUntil(context, '/main', (_) => false);
      } else {
        if (!context.mounted) return;
        _showDialog(context, '로그인 실패', '아이디 또는 비밀번호가 잘못되었습니다.');
      }
    } catch (e) {
      if (!context.mounted) return;
      _showDialog(context, '네트워크 오류', '오류 발생: $e');
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  // ── 회원 정보 로드 (마이페이지 진입 시 호출) ──────────────
  Future<void> loadMemberInfo() async {
    final mid = await secureStorage.read(key: 'mid');
    final token = await secureStorage.read(key: 'accessToken');
    if (mid == null || token == null) return;

    currentMid = mid;
    isMemberInfoLoading = true;
    notifyListeners();

    try {
      final res = await http.get(
        Uri.parse('$serverIp/api/member/me?mid=$mid'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        _applyMemberData(jsonDecode(res.body));
      }
    } catch (_) {
    } finally {
      isMemberInfoLoading = false;
      notifyListeners();
    }
  }

  void _applyMemberData(Map<String, dynamic> md) {
    memberName = md['mname'];
    memberEmail = md['email'];
    memberRegion = md['region'];
    memberRole = md['role']?.toString();
    memberRegDate = md['regDate'] != null
        ? (md['regDate'] as String).substring(0, 10)
        : null;
  }

  // ── 로그아웃 ──────────────────────────────────────────
  Future<void> showLogoutDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('로그아웃 확인'),
        content: const Text('정말 로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('취소')),
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              logout(context);
            },
            child: const Text('확인', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  Future<void> logout(BuildContext context) async {
    await secureStorage.delete(key: 'accessToken');
    await secureStorage.delete(key: 'refreshToken');
    await secureStorage.delete(key: 'profileImg');
    await secureStorage.delete(key: 'mid');
    await secureStorage.delete(key: 'memberId');

    isLoggedIn = false;
    currentMid = null;
    memberName = null;
    memberEmail = null;
    memberRegion = null;
    memberRole = null;
    memberRegDate = null;
    notifyListeners();

    if (!context.mounted) return;
    Navigator.pushNamedAndRemoveUntil(context, '/main', (_) => false);
  }

  // ── 앱 시작 시 로그인 상태 복원 ──────────────────────────
  Future<void> _checkLoginStatus() async {
    final mid = await secureStorage.read(key: 'mid');
    currentMid = mid;
    isLoggedIn = mid != null;
    notifyListeners();
  }

  void clearInputFields() {
    idController.clear();
    passwordController.clear();
    notifyListeners();
  }

  Future<String?> getAccessToken() async =>
      secureStorage.read(key: 'accessToken');

  Future<String?> getUserId() async => secureStorage.read(key: 'mid');

  void _showDialog(BuildContext context, String title, String message) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('확인')),
        ],
      ),
    );
  }

  @override
  void dispose() {
    idController.dispose();
    passwordController.dispose();
    super.dispose();
  }
}
