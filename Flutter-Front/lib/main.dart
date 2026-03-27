import 'package:busanit501_flutter_workspace_251021/controller/auth/login_controller.dart';
import 'package:busanit501_flutter_workspace_251021/controller/auth/signup_controller.dart';
import 'package:busanit501_flutter_workspace_251021/controller/todos/todo_controller.dart';
import 'package:busanit501_flutter_workspace_251021/screen/login_screen.dart';
import 'package:busanit501_flutter_workspace_251021/screen/main_screen.dart';
import 'package:busanit501_flutter_workspace_251021/my_app.dart';
import 'package:busanit501_flutter_workspace_251021/screen/my_splash.dart';
import 'package:busanit501_flutter_workspace_251021/screen/my_splash2.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'controller/ai/image/ai_image_controller.dart';
import 'controller/pd_data/food_controller.dart';
import 'screen/sample_my_app1.dart' as sampleApp;

void main() {
  // runApp( MySplash() ); // runApp
  // runApp( MyLoginScreen() ); // runApp
  // runApp( MyMainScreen() ); // runApp
  // runApp( MyAppRouting() ); // runApp
  // runApp( MyApp() ); // runApp
  // runApp( MySplash2() ); // runApp
  runApp(
      MultiProvider( // 다중 프로바이더를 사용하겠다.
        providers: [
          // 서버로부터 데이터 변경을 감지 하면 -> 화면으로 데이터를 업데이트 한다. ->
          ChangeNotifierProvider(create: (context) => FoodController()),
          // 로그인 컨트롤러 추가. 다른 구조도 같은 패턴 형식으로 진행.
          ChangeNotifierProvider(create: (context) => LoginController()),
          ChangeNotifierProvider(create: (context) => SignupController()),
          ChangeNotifierProvider(create: (context) => TodoController()),
          ChangeNotifierProvider(create: (context) => AiImageController()), // Todos 컨트롤러 추가
        ],
        child: const MyApp(),
      )
  );

} // main()
