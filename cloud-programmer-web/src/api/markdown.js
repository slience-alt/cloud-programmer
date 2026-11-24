import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'


const md = new MarkdownIt({
  html:false,
  breaks:true,
  linkify:true,
  highlight(code, language){
    if(language && hljs.getLanguage(language)){
      return hljs.highlight(
        code,
        {
          language
        }
      ).value
    }
    return hljs.highlightAuto(code).value
  }
})

export default md