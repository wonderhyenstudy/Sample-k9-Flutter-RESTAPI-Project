import 'package:flutter/material.dart';

import 'screen/my_splash2.dart';
import 'screen/tab/main_tab_screen.dart';
import 'screen/login_screen.dart';
import 'screen/signup_screen.dart';
import 'screen/mypage_screen.dart';

// 도서 관련
import 'screen/book/book_list_screen.dart';
import 'screen/book/book_detail_screen.dart';
// 대여
import 'screen/rental/rental_list_screen.dart';
// 공지사항
import 'screen/notice/notice_list_screen.dart';
import 'screen/notice/notice_detail_screen.dart';
// 행사
import 'screen/event/event_list_screen.dart';
import 'screen/event/event_detail_screen.dart';
// 1:1 문의
import 'screen/inquiry/inquiry_list_screen.dart';
import 'screen/inquiry/inquiry_write_screen.dart';
// 시설 예약
import 'screen/reserve/facility_reserve_screen.dart';
// Todo
import 'screen/todos/todos_screen.dart';
import 'screen/todos/todo_create_screen.dart';
import 'screen/todos/todo_detail_screen.dart';
// AI
import 'screen/ai/image/ai_image_screen.dart';
import 'screen/ai/stock/ai_stock_screen.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  /// 컨트롤러에서 401 응답 시 로그인 화면으로 전환하기 위한 전역 키
  static final navigatorKey = GlobalKey<NavigatorState>();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: MyApp.navigatorKey,
      debugShowCheckedModeBanner: false,
      title: '부산 도서관',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
        cardTheme: const CardTheme(elevation: 1),
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.indigo,
          foregroundColor: Colors.white,
          elevation: 1,
        ),
      ),
      // 스플래시 → 메인 탭 화면
      home: const MySplash2(),
      routes: {
        // ── 메인 탭 (로그인 전/후 공통 진입점)
        '/main': (_) => const MainTabScreen(),

        // ── 인증
        '/login': (_) => const MyLoginScreen(),
        '/signup': (_) => const SignupScreen(),
        '/mypage': (_) => const MyPageScreen(),

        // ── 도서
        '/bookList': (_) => const BookListScreen(),
        '/bookDetail': (_) => const BookDetailScreen(),

        // ── 대여
        '/rentalList': (_) => const RentalListScreen(),

        // ── 공지사항
        '/noticeList': (_) => const NoticeListScreen(),
        '/noticeDetail': (_) => const NoticeDetailScreen(),

        // ── 행사
        '/eventList': (_) => const EventListScreen(),
        '/eventDetail': (_) => const EventDetailScreen(),

        // ── 1:1 문의
        '/inquiryList': (_) => const InquiryListScreen(),
        '/inquiryWrite': (_) => const InquiryWriteScreen(),

        // ── 시설 예약
        '/facilityReserve': (_) => const FacilityReserveScreen(),

        // ── Todo
        '/todos': (_) => TodosScreen(),
        '/todoCreate': (_) => const TodoCreateScreen(),
        '/todoDetail': (context) => TodoDetailScreen(
            tno: ModalRoute.of(context)!.settings.arguments as int),

        // ── AI
        '/ai-image': (_) => AiImageScreen(),
        '/ai-stock': (_) => AiStockScreen(),
      },
    );
  }
}
