import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

void main() {
  // 실제 서버에 통신하는 통합(통신) 테스트
  group('Spring Boot API 통신 테스트 (Integration)', () {
    // 안드로이드 에뮬레이터에서는 10.0.2.2 이지만, 
    // 로컬 윈도우 환경(flutter test)에서는 127.0.0.1 (루프백)로 붙어야 합니다.
    final String baseUrl = 'http://127.0.0.1:8080';

    setUpAll(() {
      // HttpOverrides.global 처리 (혹시 모를 설정 용)
      // 실제 통신 허용 설정
    });

    test('1. API 서버 상태 확인 (Health Check / Root)', () async {
      try {
        // 서버가 켜져있는지 체크하기 위해 루트 또는 임의 엔드포인트 호출
        // 실패하더라도 Connection Exception이 나는지 확인
        final response = await http.get(Uri.parse('$baseUrl/'));
        print('Root response status: ${response.statusCode}');
        // Spring Security 때문에 401/403 이 날 수도 있지만 연결 자체는 성공한 것임.
        expect(response.statusCode, isNotNull);
      } on SocketException catch (e) {
        fail('스프링 부트 서버가 켜져 있지 않습니다. (127.0.0.1:8080 접속 실패)');
      }
    });

    test('2. 도서 목록 조회 API 통신 확인 (/api/library/book/list)', () async {
      try {
        final response = await http.get(Uri.parse('$baseUrl/api/library/book/list?page=1&size=10'));
        print('Book list API status: ${response.statusCode}');
        
        // 권한이나 서버 상태에 따라 200이 나오면 통신 기능 정상 구현됨.
        expect(
          [200, 403, 401],
          contains(response.statusCode),
          reason: '예상 범주 안의 HTTP 상태 코드가 반환되어야 합니다.'
        );

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          print('Book list count: \${data["dtoList"]?.length ?? 0}');
          expect(data, isNotNull);
        }
      } catch (e) {
        fail('데이터 통신 중 오류 발생: \$e');
      }
    });
  });
}
