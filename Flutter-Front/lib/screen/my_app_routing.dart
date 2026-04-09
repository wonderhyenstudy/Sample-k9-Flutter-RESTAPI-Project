import 'package:busanit501_flutter_workspace_251021/sample_design/list_of_listview_sample.dart';
import 'package:busanit501_flutter_workspace_251021/sample_design/navigation_mode_sample1.dart';
import 'package:busanit501_flutter_workspace_251021/sample_design/tap_mode_sample2.dart';
import 'package:busanit501_flutter_workspace_251021/screen/main_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/main_screen_%EB%A1%9C%EA%B7%B8%EC%9D%B8%20%EC%A0%95%EB%B3%B4%20%EB%B3%B4%EC%97%AC%EC%A3%BC%EA%B8%B0%EC%A0%84%20%EC%8A%A4%ED%85%8C%EC%9D%B4%ED%8A%B8%EB%A6%AC%EC%8A%A4%20%EB%B2%84%EC%A0%84.dart';
import 'package:busanit501_flutter_workspace_251021/screen/my_splash2.dart';
import 'package:busanit501_flutter_workspace_251021/screen/pd_data/food_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/signup_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todo_create_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todo_detail_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/todos/todos_screen.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'ai/image/ai_image_screen.dart';
import 'ai/stock/ai_stock_screen.dart';
import 'login_screen.dart';
import 'main_screen.dart';

class MyAppRouting extends StatelessWidget {
  const MyAppRouting({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      // 시작 화면, 스플레시 화면으로 시작. 3초뒤에 메인으로 이동함.
      home: const MySplash2(),
      //라우팅 준비물 1,
      routes: {
        '/main': (context) => MainScreen2(),
        '/signup': (context) => const SignupScreen(),
        '/login': (context) => const MyLoginScreen(),
        // 공공데이터 받아 오는 화면으로 라우팅 추가.
        // '/pdtest': (context) => const MyPdTestScreen(),
        // '/sample_design': (context) => const Sample3ListOfListView(),
        // '/sample_design2': (context) => MaterialHomePage(),
        // '/sample_design3': (context) => ResponsiveNavBarPage(),
        '/todos': (context) => TodosScreen(),
        "/todoCreate": (context) => const TodoCreateScreen(),
        '/todoDetail': (context) => TodoDetailScreen(tno: ModalRoute.of(context)!.settings.arguments as int),
        // ai 라우티.
        // "/ai-image": (context) => AiImageScreen(),
        // "/ai-stock": (context) => AiStockScreen(),

      },
    );
  }
}
