import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class MySplash2 extends StatefulWidget {
  const MySplash2({super.key});

  @override
  State<MySplash2> createState() => _MySplash2State();
}

class _MySplash2State extends State<MySplash2> {

  // 스플래쉬 기능을 하기 위해서, 스테이트풀의 생명주기를 이용해서,
  // 초기 세팅작업에서, 3초 뒤에, 메인화면으로 이동하는 코드 추가.
  @override
  void initState() { // 생명주기1번, 빌드 전에, 즉, 그리기전에
    // 초기 세팅 값을 지정을함. 그래서, 스플래쉬,.
    // 스트이트 리스 안하고, 스테이트 풀로 지정함.
    // TODO: implement initState
    super.initState();
    // 3초 뒤, 메인 이동
    Future.delayed(const Duration(seconds: 3),
            (){
      Navigator.pushReplacementNamed(context, '/main');
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      // 오른쪽 상단에 debug 문구를 제거.
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: Container(
          decoration: BoxDecoration(
              color: Colors.amberAccent
          ),
          // 간단 구성 1) 문자열 2) 이미지 구성.
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Expanded(
                child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                      style: TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w800
                      ),
                      '나의 첫 Splash 화면'
                  ),
                  Image.asset('assets/images/logo.jpg'),
                  // 공간 여백 잡는 위젯을 사용.
                  SizedBox(height: 16,),
                  CircularProgressIndicator(
                    valueColor: AlwaysStoppedAnimation(
                        Colors.white
                    ),
                  )
                ],
              ),
            ),
            ],
          ),
        ),
      ),
    );
  }
}
