import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../const/api_constants.dart';

/// 회원가입 컨트롤러
/// 두 가지 회원 시스템에 동시 가입 처리:
///   1. POST /member/register       → APIUser (JWT 로그인용)
///   2. POST /api/member/signup     → LibraryMember (도서관 API용)
class SignupController extends ChangeNotifier {
  final TextEditingController idController = TextEditingController();
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();
  final TextEditingController passwordConfirmController = TextEditingController();
  final TextEditingController mnameController = TextEditingController(); // 이름
  final TextEditingController regionController = TextEditingController(); // 지역 (선택)

  bool _isPasswordMatch = false;
  bool get isPasswordMatch => _isPasswordMatch;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  /// APIUser 기반 서버 주소 (JWT 인증 시스템)
  final String _authBaseUrl = 'http://10.0.2.2:8080';

  void validatePassword() {
    _isPasswordMatch = (passwordController.text.isNotEmpty &&
        passwordController.text == passwordConfirmController.text);
    notifyListeners();
  }

  void _showDialog(BuildContext context, String title, String message) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  void _showToast(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 2)),
    );
  }

  Future<void> checkDuplicateId(BuildContext context) async {
    final inputId = idController.text.trim();
    if (inputId.isEmpty) {
      _showDialog(context, '오류', '아이디를 입력하세요.');
      return;
    }
    try {
      final response = await http.get(
        Uri.parse('$_authBaseUrl/member/check-mid?mid=$inputId'),
      );
      if (!context.mounted) return;
      if (response.statusCode == 200) {
        _showDialog(context, '사용 가능', '이 아이디는 사용할 수 있습니다.');
      } else if (response.statusCode == 409) {
        _showDialog(context, '중복된 아이디', '이미 사용 중인 아이디입니다.');
      } else {
        _showDialog(context, '오류', '서버 응답 오류: ${response.statusCode}');
      }
    } catch (e) {
      if (!context.mounted) return;
      _showDialog(context, '오류', '네트워크 오류: $e');
    }
  }

  /// 회원 가입 처리
  /// Step 1: POST /member/register  → APIUser 생성 (JWT 로그인용)
  /// Step 2: POST /api/member/signup → LibraryMember 생성 (도서관 API용)
  Future<void> signup(BuildContext context) async {
    if (!_isPasswordMatch) {
      _showDialog(context, '오류', '비밀번호가 일치하지 않습니다.');
      return;
    }

    final mid = idController.text.trim();
    final mpw = passwordController.text.trim();
    final email = emailController.text.trim();
    final mname = mnameController.text.trim();
    final region = regionController.text.trim();

    if (mid.isEmpty || mpw.isEmpty || email.isEmpty || mname.isEmpty) {
      _showToast(context, '아이디, 이름, 이메일, 비밀번호를 모두 입력하세요.');
      return;
    }

    _isLoading = true;
    notifyListeners();

    try {
      // Step 1: APIUser 생성 (JWT 인증 시스템)
      final authResponse = await http.post(
        Uri.parse('$_authBaseUrl/member/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'mid': mid, 'mpw': mpw, 'email': email}),
      );

      if (!context.mounted) return;

      if (authResponse.statusCode != 200) {
        final body = utf8.decode(authResponse.bodyBytes);
        _showToast(context, '회원 가입 실패 (인증): $body');
        return;
      }

      // Step 2: LibraryMember 생성 (도서관 시스템)
      final libraryResponse = await http.post(
        Uri.parse('${ApiConstants.springBaseUrl}/member/signup'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'mid': mid,
          'mpw': mpw,
          'mpwConfirm': passwordConfirmController.text.trim(),
          'mname': mname,
          'email': email,
          'region': region.isEmpty ? null : region,
        }),
      );

      if (!context.mounted) return;

      if (libraryResponse.statusCode == 200 || libraryResponse.statusCode == 201) {
        _showToast(context, '회원 가입 성공!');
        Future.delayed(const Duration(milliseconds: 800), () {
          if (context.mounted) Navigator.pushReplacementNamed(context, '/login');
        });
      } else {
        final body = utf8.decode(libraryResponse.bodyBytes);
        _showToast(context, '회원 가입 실패 (도서관): $body');
      }
    } catch (e) {
      if (!context.mounted) return;
      _showToast(context, '오류 발생: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    idController.dispose();
    emailController.dispose();
    passwordController.dispose();
    passwordConfirmController.dispose();
    mnameController.dispose();
    regionController.dispose();
    super.dispose();
  }
}
