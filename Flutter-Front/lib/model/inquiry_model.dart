class InquiryModel {
  final int? id;
  final String? title;
  final String? content;
  final String? writer;
  final bool? isReplied;
  final String? regDate;

  InquiryModel({this.id, this.title, this.content, this.writer, this.isReplied, this.regDate});

  factory InquiryModel.fromJson(Map<String, dynamic> json) {
    return InquiryModel(
      id: json['id'],
      title: json['title'],
      content: json['content'],
      writer: json['writer'],
      isReplied: json['answered'] ?? json['isReplied'], // Spring: answered
      regDate: json['regDate'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'content': content,
      'writer': writer,
      'isReplied': isReplied,
      'regDate': regDate,
    };
  }
}
