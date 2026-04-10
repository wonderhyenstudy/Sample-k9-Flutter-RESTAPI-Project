import 'package:flutter/material.dart';

/// 앱 공통 베이스 레이아웃
/// 상세 화면(Navigator.push 대상)에서 일관된 Scaffold 구조를 제공합니다.
class AppBaseLayout extends StatelessWidget {
  final String title;
  final Widget body;
  final List<Widget>? actions;
  final Widget? floatingActionButton;
  final Widget? bottomNavigationBar;
  final bool showBackButton;

  const AppBaseLayout({
    super.key,
    required this.title,
    required this.body,
    this.actions,
    this.floatingActionButton,
    this.bottomNavigationBar,
    this.showBackButton = true,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        centerTitle: true,
        automaticallyImplyLeading: showBackButton,
        actions: actions,
        elevation: 1,
      ),
      body: SafeArea(child: body),
      floatingActionButton: floatingActionButton,
      bottomNavigationBar: bottomNavigationBar,
    );
  }
}

/// 로그인이 필요한 탭에서 표시하는 유도 위젯
class LoginRequiredWidget extends StatelessWidget {
  final VoidCallback onLoginTap;

  const LoginRequiredWidget({super.key, required this.onLoginTap});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.lock_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            const Text(
              '로그인이 필요한 서비스입니다.',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              '로그인 후 이용해주세요.',
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: onLoginTap,
              icon: const Icon(Icons.login),
              label: const Text('로그인 하러 가기'),
            ),
          ],
        ),
      ),
    );
  }
}
