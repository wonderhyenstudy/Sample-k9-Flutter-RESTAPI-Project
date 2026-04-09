class LibraryEventModel {
  final int? id;
  final String? title;
  final String? content;
  final String? eventDate;
  final String? location;

  LibraryEventModel({this.id, this.title, this.content, this.eventDate, this.location});

  factory LibraryEventModel.fromJson(Map<String, dynamic> json) {
    return LibraryEventModel(
      id: json['id'],
      title: json['title'],
      content: json['content'],
      eventDate: json['startDate'] ?? json['eventDate'], // Spring: startDate
      location: json['location'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'content': content,
      'eventDate': eventDate,
      'location': location,
    };
  }
}
