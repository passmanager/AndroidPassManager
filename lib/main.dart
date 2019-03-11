import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cached_network_image/cached_network_image.dart';

void main() {
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(App());
}

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(primaryColor: Colors.blue),
      home: LoginPage(),
      title: "Android Password Manager",
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  GlobalKey<ScaffoldState> key = GlobalKey();
  var _services = [
    "Facebook",
    "Instagram",
    "Viber",
    "Tumblr",
    "Spotify",
    "Apple",
    "Microsoft",
    "IBM",
    "Google+",
    "Twitter",
    "WhatsApp",
    "Pinterest",
    "YouTube"
  ];
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {},
      ),
      endDrawer: Drawer(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                children: <Widget>[
                  UserAccountsDrawerHeader(
                    accountEmail: Text("Generic Name"),
                    accountName: Text("Generic Name"),
                  ),
                  ListTile(
                    title: Text("Logout"),
                    onTap: () {
                      Navigator.pop(context);
                      print("Hi");
                    },
                  ),
                ],
              ),
            ),
            Container(
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(width: 3, color: Colors.grey),
                ),
              ),
              child: ListTile(
                onTap: () {},
                title: Text("Settings"),
                trailing: Icon(Icons.settings),
              ),
            ),
          ],
        ),
      ),
      key: key,
      appBar: AppBar(
        automaticallyImplyLeading: false,
        elevation: 1,
        title: Text("Password Manager"),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.dehaze),
            onPressed: () {
              key.currentState.openEndDrawer();
            },
          ),
        ],
      ),
      body: Container(
        child: ListView(
          children: _services.map((i) => CustomListTile(i)).toList(),
        ),
      ),
    );
  }
}

class LoginPage extends StatefulWidget {
  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Expanded(
              flex: 6,
              child: Padding(
                padding: const EdgeInsets.all(60.0),
                child: FittedBox(
                  child: FlutterLogo(),
                ),
              ),
            ),
            Expanded(
              flex: 7,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(
                  children: <Widget>[
                    TextField(
                      decoration: InputDecoration(
                        prefixIcon: Icon(Icons.vpn_key),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                        labelText: "Master Key",
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: RaisedButton(
                textColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
                color: Colors.blue,
                elevation: 1,
                child: Text("Log in"),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => HomePage(),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class CustomListTile extends StatelessWidget {
  @override
  String serviceName = "";
  CustomListTile(this.serviceName);

  Widget build(BuildContext context) {
    var avatar = CachedNetworkImage(
        imageUrl: "https://logo.clearbit.com/${serviceName.toLowerCase()}.com",
        placeholder: (context, url) => CircularProgressIndicator(),
        errorWidget: (context, url, error) => new Icon(Icons.error_outline));

    Color foreground;
    if (Theme.of(context).brightness == Brightness.dark) {
      foreground = Colors.white;
    } else {
      foreground = Theme.of(context).primaryColor;
    }
    if (serviceName != "") {
      return Container(
        child: InkWell(
          onTap: () {},
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            child: Row(
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: CircleAvatar(
                    backgroundColor: Colors.white,
                    radius: 20.0,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(50.0),
                      child: avatar,
                    ),
                  ),
                ),
                Text(
                  serviceName,
                  style: TextStyle(
                      fontSize: 16.0,
                      fontWeight: FontWeight.bold,
                      color: foreground),
                )
              ],
            ),
          ),
        ),
      );
    } else {
      return ListTile();
    }
  }
}

class ServicePage extends StatefulWidget {
  @override
  _ServicePageState createState() => _ServicePageState();
}

class _ServicePageState extends State<ServicePage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Container(),
      endDrawer: Drawer(),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        child: Icon(Icons.arrow_back),
      ),
    );
  }
}
