import 'package:busanit501_flutter_workspace_251021/screen/main_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/my_splash2.dart';
import 'package:busanit501_flutter_workspace_251021/screen/signup_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todo_create_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todo_detail_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todos_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/mypage_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/book/book_list_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/book/book_detail_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/rental/rental_list_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/notice/notice_list_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/notice/notice_detail_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/event/event_list_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/event/event_detail_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/inquiry/inquiry_list_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/inquiry/inquiry_write_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/reserve/facility_reserve_screen.dart';
import 'package:flutter/material.dart';

import 'package:busanit501_flutter_workspace_251021/screen/ai/image/ai_image_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/ai/stock/ai_stock_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/login_screen.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const MySplash2(),
      routes: {
        // 기본 인증 화면
        '/main':      (context) => const MainScreen2(),
        '/signup':    (context) => const SignupScreen(),
        '/login':     (context) => const MyLoginScreen(),
        '/mypage':    (context) => const MyPageScreen(),
        // Todo 화면
        '/todos':     (context) => TodosScreen(),
        '/todoCreate':(context) => const TodoCreateScreen(),
        '/todoDetail':(context) => TodoDetailScreen(
            tno: ModalRoute.of(context)!.settings.arguments as int),
        // 도서 관련
        '/bookList':  (context) => const BookListScreen(),
        '/bookDetail':(context) => const BookDetailScreen(),
        // 대여 현황
        '/rentalList':(context) => const RentalListScreen(),
        // 공지사항
        '/noticeList':   (context) => const NoticeListScreen(),
        '/noticeDetail': (context) => const NoticeDetailScreen(),
        // 행사
        '/eventList':   (context) => const EventListScreen(),
        '/eventDetail': (context) => const EventDetailScreen(),
        // 1:1 문의
        '/inquiryList': (context) => const InquiryListScreen(),
        '/inquiryWrite':(context) => const InquiryWriteScreen(),
        // 시설 예약
        '/facilityReserve': (context) => const FacilityReserveScreen(),
        // AI 기능
        '/ai-image':  (context) => AiImageScreen(),
        '/ai-stock':  (context) => AiStockScreen(),
      },
    );
  }
}
