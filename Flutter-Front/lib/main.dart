import 'package:busanit501_flutter_workspace_251021/controller/auth/login_controller.dart';
import 'package:busanit501_flutter_workspace_251021/controller/auth/signup_controller.dart';
import 'package:busanit501_flutter_workspace_251021/controller/todos/todo_controller.dart';
import 'package:busanit501_flutter_workspace_251021/my_app.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'controller/admin/admin_book_controller.dart';
import 'controller/ai/image/ai_image_controller.dart';
import 'controller/pd_data/food_controller.dart';
import 'controller/book_controller.dart';
import 'controller/rental_controller.dart';
import 'controller/notice_controller.dart';
import 'controller/event_controller.dart';
import 'controller/inquiry_controller.dart';
import 'controller/reserve_controller.dart';

void main() {
  runApp(
      MultiProvider( // 다중 프로바이더를 사용하겠다.
        providers: [
          // 서버로부터 데이터 변경을 감지 하면 -> 화면으로 데이터를 업데이트 한다. ->
          ChangeNotifierProvider(create: (context) => FoodController()),
          // 로그인 컨트롤러 추가. 다른 구조도 같은 패턴 형식으로 진행.
          ChangeNotifierProvider(create: (context) => LoginController()),
          ChangeNotifierProvider(create: (context) => SignupController()),
          ChangeNotifierProvider(create: (context) => TodoController()),
          ChangeNotifierProvider(create: (context) => AiImageController()),
          // 도서관 관련 컨트롤러
          ChangeNotifierProvider(create: (context) => BookController()),
          ChangeNotifierProvider(create: (context) => RentalController()),
          ChangeNotifierProvider(create: (context) => NoticeController()),
          ChangeNotifierProvider(create: (context) => EventController()),
          ChangeNotifierProvider(create: (context) => InquiryController()),
          ChangeNotifierProvider(create: (context) => ReserveController()),
          //관리자
          // ── 관리자 도서 컨트롤러를 여기에 추가 ──
          ChangeNotifierProvider(create: (_) => AdminBookController()),
        ],
        child: const MyApp(),
      )
  );

} // main()
