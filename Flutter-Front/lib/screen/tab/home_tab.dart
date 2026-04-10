import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../controller/auth/login_controller.dart';
import '../../controller/notice_controller.dart';
import '../../controller/event_controller.dart';
import '../../widget/common/section_header.dart';
import '../../widget/common/library_card_tile.dart';
import '../../widget/common/loading_widget.dart';

/// 홈 탭
/// - PageView 배너 (자동 슬라이드)
/// - 비로그인: 로그인/회원가입 버튼
/// - 공지사항 미리보기 3건
/// - 이벤트 미리보기 3건
class HomeTab extends StatefulWidget {
  const HomeTab({super.key});

  @override
  State<HomeTab> createState() => _HomeTabState();
}

class _HomeTabState extends State<HomeTab> {
  final PageController _bannerController = PageController();
  Timer? _bannerTimer;
  int _bannerIndex = 0;

  static const List<_BannerData> _banners = [
    _BannerData(
      color: Color(0xFF1565C0),
      icon: Icons.local_library,
      title: '부산 도서관에 오신 것을\n환영합니다',
      subtitle: '다양한 도서와 서비스를 이용해보세요',
    ),
    _BannerData(
      color: Color(0xFF2E7D32),
      icon: Icons.event,
      title: '이번 달 특별 행사',
      subtitle: '독서 토론, 작가 강연 등 다채로운 프로그램',
    ),
    _BannerData(
      color: Color(0xFF6A1B9A),
      icon: Icons.auto_awesome,
      title: 'AI 도서 추천 서비스',
      subtitle: '취향에 맞는 도서를 AI가 추천해드립니다',
    ),
  ];

  @override
  void initState() {
    super.initState();
    _startBannerTimer();
    Future.microtask(() {
      if (!mounted) return;
      context.read<NoticeController>().fetchNotices();
      context.read<EventController>().fetchEvents();
    });
  }

  void _startBannerTimer() {
    _bannerTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      if (!mounted) return;
      final next = (_bannerIndex + 1) % _banners.length;
      _bannerController.animateToPage(
        next,
        duration: const Duration(milliseconds: 400),
        curve: Curves.easeInOut,
      );
    });
  }

  @override
  void dispose() {
    _bannerTimer?.cancel();
    _bannerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isLoggedIn = context.watch<LoginController>().isLoggedIn;

    return RefreshIndicator(
      onRefresh: () async {
        context.read<NoticeController>().fetchNotices();
        context.read<EventController>().fetchEvents();
      },
      child: ListView(
        children: [
          // ── 배너 PageView ─────────────────────────────
          SizedBox(
            height: 180,
            child: Stack(
              children: [
                PageView.builder(
                  controller: _bannerController,
                  itemCount: _banners.length,
                  onPageChanged: (i) => setState(() => _bannerIndex = i),
                  itemBuilder: (_, i) => _BannerCard(data: _banners[i]),
                ),
                // 인디케이터
                Positioned(
                  bottom: 10,
                  left: 0,
                  right: 0,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      _banners.length,
                      (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        width: _bannerIndex == i ? 20 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: _bannerIndex == i
                              ? Colors.white
                              : Colors.white54,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // ── 비로그인 상태 ─────────────────────────────
          if (!isLoggedIn) ...[
            const SizedBox(height: 16),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      const Text('서비스를 이용하려면 로그인하세요.',
                          style: TextStyle(fontSize: 15)),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: ElevatedButton(
                              onPressed: () =>
                                  Navigator.pushNamed(context, '/login'),
                              child: const Text('로그인'),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: OutlinedButton(
                              onPressed: () =>
                                  Navigator.pushNamed(context, '/signup'),
                              child: const Text('회원 가입'),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],

          // ── 공지사항 미리보기 ─────────────────────────
          SectionHeader(
            title: '공지사항',
            onMoreTap: () => Navigator.pushNamed(context, '/noticeList'),
          ),
          Consumer<NoticeController>(
            builder: (_, ctrl, __) {
              if (ctrl.isLoading) return const LoadingWidget();
              if (ctrl.noticeList.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Center(
                      child: Text('공지사항이 없습니다.',
                          style: TextStyle(color: Colors.grey))),
                );
              }
              final preview = ctrl.noticeList.take(3).toList();
              return Column(
                children: preview
                    .map((n) => LibraryCardTile(
                          leadingIcon: Icons.campaign_outlined,
                          iconColor: Colors.indigo,
                          title: n.title ?? '공지',
                          subtitle: n.regDate,
                          trailingText: '조회 ${n.viewCount ?? 0}',
                          onTap: () => Navigator.pushNamed(
                              context, '/noticeDetail',
                              arguments: n.id),
                        ))
                    .toList(),
              );
            },
          ),

          // ── 이벤트 미리보기 ───────────────────────────
          SectionHeader(
            title: '이벤트 / 행사',
            onMoreTap: () => Navigator.pushNamed(context, '/eventList'),
          ),
          Consumer<EventController>(
            builder: (_, ctrl, __) {
              if (ctrl.isLoading) return const LoadingWidget();
              if (ctrl.eventList.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Center(
                      child: Text('등록된 행사가 없습니다.',
                          style: TextStyle(color: Colors.grey))),
                );
              }
              final preview = ctrl.eventList.take(3).toList();
              return Column(
                children: preview
                    .map((e) => LibraryCardTile(
                          leadingIcon: Icons.celebration_outlined,
                          iconColor: Colors.orange,
                          title: e.title ?? '행사',
                          subtitle: e.eventDate,
                          onTap: () => Navigator.pushNamed(
                              context, '/eventDetail',
                              arguments: e.id),
                        ))
                    .toList(),
              );
            },
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _BannerCard extends StatelessWidget {
  final _BannerData data;
  const _BannerCard({required this.data});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [data.color, data.color.withOpacity(0.75)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      padding: const EdgeInsets.all(24),
      child: Row(
        children: [
          Icon(data.icon, size: 64, color: Colors.white70),
          const SizedBox(width: 20),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(data.title,
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        height: 1.4)),
                const SizedBox(height: 8),
                Text(data.subtitle,
                    style: const TextStyle(
                        color: Colors.white70, fontSize: 13)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BannerData {
  final Color color;
  final IconData icon;
  final String title;
  final String subtitle;
  const _BannerData(
      {required this.color,
      required this.icon,
      required this.title,
      required this.subtitle});
}
