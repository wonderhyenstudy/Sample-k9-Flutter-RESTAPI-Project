import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../controller/auth/login_controller.dart';
import 'home_tab.dart';
import 'book_tab.dart';
import 'my_service_tab.dart';
import 'ai_tab.dart';
import 'mypage_tab.dart';

/// 앱 메인 탭 컨테이너
/// BottomNavigationBar 5탭 구조:
///   0: 홈  1: 도서  2: 내 서비스  3: AI  4: 마이페이지
class MainTabScreen extends StatefulWidget {
  const MainTabScreen({super.key});

  @override
  State<MainTabScreen> createState() => _MainTabScreenState();
}

class _MainTabScreenState extends State<MainTabScreen> {
  int _currentIndex = 0;

  static const List<_TabItem> _tabs = [
    _TabItem(label: '홈',      icon: Icons.home_outlined,         activeIcon: Icons.home),
    _TabItem(label: '도서',    icon: Icons.menu_book_outlined,    activeIcon: Icons.menu_book),
    _TabItem(label: '내 서비스', icon: Icons.folder_outlined,      activeIcon: Icons.folder),
    _TabItem(label: 'AI',     icon: Icons.auto_awesome_outlined,  activeIcon: Icons.auto_awesome),
    _TabItem(label: 'MY',     icon: Icons.person_outline,         activeIcon: Icons.person),
  ];

  static const List<String> _titles = ['도서관 홈', '도서 검색', '내 서비스', 'AI 기능', '마이페이지'];

  /// 로그인 필요 탭 인덱스 (홈=0 제외)
  static const Set<int> _requireLogin = {1, 2, 3, 4};

  void _onTabTap(int index) {
    final isLoggedIn = context.read<LoginController>().isLoggedIn;
    if (_requireLogin.contains(index) && !isLoggedIn) {
      _showLoginDialog();
      return;
    }
    setState(() => _currentIndex = index);
  }

  void _showLoginDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('로그인 필요'),
        content: const Text('해당 서비스는 로그인 후 이용할 수 있습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('취소'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              Navigator.pushNamed(context, '/login');
            },
            child: const Text('로그인'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLoggedIn = context.watch<LoginController>().isLoggedIn;

    // 로그아웃 시 홈으로 복귀
    if (!isLoggedIn && _requireLogin.contains(_currentIndex)) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) setState(() => _currentIndex = 0);
      });
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_titles[_currentIndex],
            style: const TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
        elevation: 1,
        actions: [
          if (isLoggedIn)
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: '로그아웃',
              onPressed: () =>
                  context.read<LoginController>().showLogoutDialog(context),
            )
          else
            TextButton(
              onPressed: () => Navigator.pushNamed(context, '/login'),
              child: const Text('로그인', style: TextStyle(color: Colors.white)),
            ),
        ],
      ),
      // IndexedStack: 탭 전환 시 상태 보존
      body: IndexedStack(
        index: _currentIndex,
        children: const [
          HomeTab(),
          BookTab(),
          MyServiceTab(),
          AiTab(),
          MyPageTab(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: _onTabTap,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: _tabs
            .map((t) => NavigationDestination(
                  icon: Icon(t.icon),
                  selectedIcon: Icon(t.activeIcon),
                  label: t.label,
                ))
            .toList(),
      ),
    );
  }
}

class _TabItem {
  final String label;
  final IconData icon;
  final IconData activeIcon;
  const _TabItem(
      {required this.label, required this.icon, required this.activeIcon});
}
