import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../controller/auth/login_controller.dart';

/// 마이페이지 화면 (회원의 개인 정보, 대여 현황, 문의 내역 등)
class MyPageScreen extends StatelessWidget {
  const MyPageScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final loginController = context.watch<LoginController>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('마이페이지'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          ListTile(
            leading: const Icon(Icons.person, size: 40),
            title: Text(loginController.idController.text.isNotEmpty
                ? loginController.idController.text
                : '로그인된 사용자'),
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.book),
            title: const Text('내 대여 내역 보기'),
            onTap: () => Navigator.pushNamed(context, '/rentalList'),
          ),
          ListTile(
            leading: const Icon(Icons.question_answer),
            title: const Text('1:1 문의 내역'),
            onTap: () => Navigator.pushNamed(context, '/inquiryList'),
          ),
          ListTile(
            leading: const Icon(Icons.logout),
            title: const Text('로그아웃'),
            onTap: () => loginController.showLogoutDialog(context),
          ),
        ],
      ),
    );
  }
}
