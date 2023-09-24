import 'package:flutter/material.dart';
import 'dart:io';

void main() {
  runApp(MainApp());
}

class MainApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: MyHomePage(),
      theme: ThemeData(brightness: Brightness.dark),
    );
  }
}

class MyHomePage extends StatelessWidget {
  void runPythonCode(BuildContext context) {
    Process.run('python',
        ['C:/Users/dylan/Documents/repos/SWE-Butler-App/braille_v1.py']);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Center(
            child: Padding(
      padding: EdgeInsets.all(2.0),
      child: FractionallySizedBox(
        widthFactor: 1.0, // width w.r.t to parent
        heightFactor: 1.0, // height w.r.t to parent:
        child: IconButton(
          onPressed: () => runPythonCode(context),
          icon: Image.asset('assets/images/icons/BrailleLogo2.png'),
        ),
      ),
    )));
  }
}
