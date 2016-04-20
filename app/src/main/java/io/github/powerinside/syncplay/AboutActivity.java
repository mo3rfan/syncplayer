package io.github.powerinside.syncplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView about = (TextView) findViewById(R.id.abouttextview);

        about.setText("SyncPlayer is an unofficial app that aims to be 100% compatible with the Syncplay API."
        + "\n\nOPEN SOURCE LICENSES\n=====================\nckChangelog:-\n" +
                "Copyright (C) 2012-2015 cketti and contributors\n" +
                        "\n" +
                        "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                        "you may not use this file except in compliance with the License.\n" +
                        "You may obtain a copy of the License at\n" +
                        "\n" +
                        "http://www.apache.org/licenses/LICENSE-2.0\n" +
                        "\n" +
                        "Unless required by applicable law or agreed to in writing, software\n" +
                        "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                        "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                        "See the License for the specific language governing permissions and\n" +
                        "limitations under the License."
                + "\n\n ============================ \n\n" + "Android-rate: " +
                "The MIT License (MIT)\n" +
                        "\n" +
                        "Copyright (c) 2015 Shintaro Katafuchi\n" +
                        "\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                        "of this software and associated documentation files (the \"Software\"), to deal\n" +
                        "in the Software without restriction, including without limitation the rights\n" +
                        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                        "copies of the Software, and to permit persons to whom the Software is\n" +
                        "furnished to do so, subject to the following conditions:\n" +
                        "\n" +
                        "The above copyright notice and this permission notice shall be included in\n" +
                        "all copies or substantial portions of the Software.\n" +
                        "\n" +
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                        "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                        "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                        "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN\n" +
                        "THE SOFTWARE."
        );
    }

}
